package hr.fer.zemris.projekt.gui;

import hr.fer.zemris.projekt.exceptions.FileFormatException;
import hr.fer.zemris.projekt.gui.filters.ImageFilter;
import hr.fer.zemris.projekt.gui.icons.Icons;
import hr.fer.zemris.projekt.gui.listeners.IBoundingBoxActionListener;
import hr.fer.zemris.projekt.gui.listeners.IBoundingBoxModelChangeListener;
import hr.fer.zemris.projekt.gui.listeners.IDrawingStatusListener;
import hr.fer.zemris.projekt.gui.listeners.JListKeyNavigationListener;
import hr.fer.zemris.projekt.gui.models.BoxPredictionViewModel;
import hr.fer.zemris.projekt.gui.models.LabeledImageModel;
import hr.fer.zemris.projekt.gui.panels.*;
import hr.fer.zemris.projekt.gui.providers.MessageProvider;
import hr.fer.zemris.projekt.gui.renderers.ColoredListRenderer;
import hr.fer.zemris.projekt.gui.services.RandomColorChooser;
import hr.fer.zemris.projekt.image.managers.ImageManager;
import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.image.segmentation.ConnectedComponent;
import hr.fer.zemris.projekt.neural.ConvNetClassifier;
import hr.fer.zemris.projekt.neural.INetwork;
import hr.fer.zemris.projekt.neural.exceptions.RetrainNetworkException;
import hr.fer.zemris.projekt.parser.JVCFileParser;
import org.apache.commons.math3.util.Pair;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

public class JLabelingSystem extends JFrame implements IBoundingBoxModelChangeListener, IBoundingBoxActionListener {

    private static final String ERROR = "error";
    private static final String WARNING = "warning";
    private static final String INFORMATION = "information";
    private static final String NETWORK_PATH = "src/main/resources/digits-model_extended_6.zip";


    private INetwork net;
    private MessageProvider provider = MessageProvider.getInstance();


    private JList<String> imagesInSelectedDirectory;
    private JList<BoxPredictionViewModel> boundingBoxes;
    private List<IDrawingStatusListener> listeners;

    private JPanel imagePanel;
    private JPanel boundingBoxPanel;

    private JToggleButton addBoundingBoxBtn;

    private Map<String, Path> imagesByName;
    private Map<Path, LabeledImageModel> classifiedImages;
    private Map<Path, LabeledImageModel> loadedNotClassifiedImages;


    private JLayeredPane layeredPane;
    private boolean modified;


    public JLabelingSystem() {
        setLocation(0, 0);
        setSize(1200, 800);
        setTitle(provider.get("title"));
        setLayout(new BorderLayout());

        try {
            net = new ConvNetClassifier(new File(NETWORK_PATH));
        } catch (IOException e) {
            showNeuralClassifierError();
        }


        layeredPane = new JLayeredPane();

        imagesInSelectedDirectory = new JList<>();
        boundingBoxes = new JList<>();

        imagesByName = new HashMap<>();
        classifiedImages = new HashMap<>();
        loadedNotClassifiedImages = new HashMap<>();

        listeners = new ArrayList<>();


        imagePanel = new ImagePanel();
        boundingBoxPanel = new BoundingBoxPanel(this, this);
        listeners.add(((BoundingBoxPanel) boundingBoxPanel));

        initActions();
        initMenus();
        initGui();

        addWindowListener();
        addResizeListener();
    }

    private void addResizeListener() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                imagePanel.setBounds(0, 0, getWidth(), getHeight());
                boundingBoxPanel.setBounds(0, 0, getWidth(), getHeight());
            }
        });
    }


    private void initGui() {

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout());

        JButton previousImageBtn = new JButton(previousImage);
        JButton nextImageBtn = new JButton(nextImage);
        addBoundingBoxBtn = new JToggleButton(addBoundingBoxAction);
        JButton retrainNetworkBtn = new JButton(retrainNetwork);
        JButton zoomInBtn = new JButton(zoomIn);
        JButton zoomOutBtn = new JButton(zoomOut);

        previousImage.setEnabled(false);
        nextImage.setEnabled(false);
        addBoundingBoxBtn.setEnabled(false);
        retrainNetwork.setEnabled(false);
        zoomIn.setEnabled(false);
        zoomOut.setEnabled(false);

        buttonsPanel.add(previousImageBtn);
        buttonsPanel.add(nextImageBtn);
        buttonsPanel.add(addBoundingBoxBtn);
        buttonsPanel.add(retrainNetworkBtn);
        buttonsPanel.add(zoomInBtn);
        buttonsPanel.add(zoomOutBtn);


        JScrollPane pane = new JScrollPane(layeredPane);
        pane.getVerticalScrollBar().setUnitIncrement(16);
        pane.getHorizontalScrollBar().setUnitIncrement(16);

        add(pane, BorderLayout.CENTER);


        imagePanel.setOpaque(false);
        imagePanel.setBounds(0, 0, getWidth(), getHeight());


        boundingBoxPanel.setOpaque(false);
        boundingBoxPanel.setBounds(0, 0, getWidth(), getHeight());

        layeredPane.add(imagePanel, JLayeredPane.FRAME_CONTENT_LAYER);
        layeredPane.add(boundingBoxPanel, JLayeredPane.MODAL_LAYER);
        adjustScrollPaneComponentListener(pane);

        add(buttonsPanel, BorderLayout.PAGE_START);

        imagesInSelectedDirectory.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        boundingBoxes.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        boundingBoxes.setCellRenderer(new ColoredListRenderer());

        addListenerToList();
        addListenerToBoundingBoxesList();

        layeredPane.addMouseMotionListener((BoundingBoxPanel) boundingBoxPanel);
        layeredPane.addMouseListener((BoundingBoxPanel) boundingBoxPanel);

        addBoundingBoxBtn.addItemListener(e -> {
            for (IDrawingStatusListener listener : listeners) {
                listener.statusChanged(e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new GridLayout(2, 0));
        listPanel.add(new JScrollPane(boundingBoxes));
        listPanel.add(new JScrollPane(imagesInSelectedDirectory));


        add(listPanel, BorderLayout.LINE_END);
    }

    private void adjustScrollPaneComponentListener(JScrollPane pane) {
        imagePanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                layeredPane.setPreferredSize(e.getComponent().getSize());
                pane.revalidate();
                pane.repaint();
            }
        });
    }


    private void initActions() {
        Icons icons = Icons.getInstance();
        nextImage.putValue(Action.SMALL_ICON, icons.getIcon("next"));
        previousImage.putValue(Action.SMALL_ICON, icons.getIcon("previous"));
        addBoundingBoxAction.putValue(Action.SMALL_ICON, icons.getIcon("box"));
        retrainNetwork.putValue(Action.SMALL_ICON, icons.getIcon("retrain"));
        zoomIn.putValue(Action.SMALL_ICON, icons.getIcon("zoom-in"));
        zoomOut.putValue(Action.SMALL_ICON, icons.getIcon("zoom-out"));
        saveDataset.putValue(Action.SMALL_ICON, icons.getIcon("save"));
        exitAction.putValue(Action.SMALL_ICON, icons.getIcon("exit"));
        directoryChooser.putValue(Action.SMALL_ICON, icons.getIcon("open"));
        loadDataset.putValue(Action.SMALL_ICON, icons.getIcon("load"));
    }

    private void initMenus() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        menuBar.add(fileMenu);
        fileMenu.add(directoryChooser);
        fileMenu.add(loadDataset);

        saveDataset.setEnabled(false);
        loadDataset.setEnabled(false);

        fileMenu.add(saveDataset);
        fileMenu.add(exitAction);

        setJMenuBar(menuBar);
    }


    private void showNeuralClassifierError() {
        JOptionPane.showMessageDialog(JLabelingSystem.this, provider.get("network_error"),
                provider.get(ERROR), JOptionPane.ERROR_MESSAGE);
    }


    private AbstractAction exitAction = new AbstractAction(provider.get("exit")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            exitAction();
        }
    };

    private AbstractAction zoomIn = new AbstractAction(provider.get("zoom_in")) {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            ((ZoomablePanel) imagePanel).zoomIn();
            ((ZoomablePanel) boundingBoxPanel).zoomIn();

        }
    };

    private AbstractAction zoomOut = new AbstractAction(provider.get("zoom_out")) {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            ((ZoomablePanel) imagePanel).zoomOut();
            ((ZoomablePanel) boundingBoxPanel).zoomOut();
        }
    };

    private AbstractAction directoryChooser = new AbstractAction(provider.get("select_directory")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean clear = false;
            if (modified) {
                int answer = JOptionPane.showConfirmDialog(JLabelingSystem.this,
                        provider.get("save_before_change_directory"), provider.get("save_changes"),
                        JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.YES_OPTION) {
                    saveDataset();
                }
                clear = true;
            }


            JFileChooser directoryChooser = new JFileChooser();
            directoryChooser.setDialogTitle(provider.get("select_folder_with_images"));
            directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            directoryChooser.setAcceptAllFileFilterUsed(false);

            if (directoryChooser.showOpenDialog(JLabelingSystem.this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            if (clear) {
                clearAll();
            }

            File directory = directoryChooser.getSelectedFile();

            DefaultListModel<String> imagesInDirectoryModel = new DefaultListModel<>();
            Set<String> imagesSet = new TreeSet<>();
            FilenameFilter imageFilter = new ImageFilter();
            if (directory.isDirectory()) {
                File[] images = directory.listFiles(imageFilter);
                if (images != null) {
                    for (File image : images) {
                        imagesSet.add(image.getName());
                        imagesByName.put(image.getName(), image.toPath());
                    }
                }
            }
            if (imagesByName.size() == 0) {
                JOptionPane.showMessageDialog(JLabelingSystem.this, provider.get("no_images_in_dir"),
                        provider.get(INFORMATION), JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            for (String image : imagesSet) {
                imagesInDirectoryModel.addElement(image);
            }

            loadDataset.setEnabled(true);
            modified = true;


            imagesInSelectedDirectory.setModel(imagesInDirectoryModel);
            if (imagesByName.size() > 0) {
                imagesInSelectedDirectory.setSelectedIndex(0);
                enableActions();
            } else {
                disableActions();
            }
            imagesInSelectedDirectory.revalidate();
            imagesInSelectedDirectory.repaint();
        }
    };

    private void disableActions() {
        nextImage.setEnabled(false);
        previousImage.setEnabled(false);
        addBoundingBoxBtn.setEnabled(false);
        retrainNetwork.setEnabled(false);
        zoomIn.setEnabled(false);
        zoomOut.setEnabled(false);
    }

    private void enableActions() {
        nextImage.setEnabled(true);
        previousImage.setEnabled(true);
        addBoundingBoxBtn.setEnabled(true);
        retrainNetwork.setEnabled(true);
        zoomIn.setEnabled(true);
        zoomOut.setEnabled(true);
    }

    private void clearAll() {
        classifiedImages.clear();
        imagesByName.clear();
        ((DefaultListModel<BoxPredictionViewModel>) boundingBoxes.getModel()).removeAllElements();
        ((DefaultListModel<String>) imagesInSelectedDirectory.getModel()).removeAllElements();
    }


    private AbstractAction loadDataset = new AbstractAction(provider.get("load_dataset")) {
        @Override
        public void actionPerformed(ActionEvent e) {

            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle(provider.get("open_jvc_file"));
            FileNameExtensionFilter filter = new FileNameExtensionFilter(provider.get("bb_description_file"), "jvc");
            fc.setAcceptAllFileFilterUsed(false);
            fc.setFileFilter(filter);

            if (fc.showOpenDialog(JLabelingSystem.this) != JFileChooser.APPROVE_OPTION) {
                return;
            }


            File fileName = fc.getSelectedFile();
            Path filePath = fileName.toPath();
            if (!Files.isReadable(filePath) || !fileName.toString().endsWith(".jvc")) {
                JOptionPane.showMessageDialog(JLabelingSystem.this, provider.get("the_file")
                        + fileName.getAbsolutePath()
                        + provider.get("can_not_be_loaded"), provider.get(ERROR), JOptionPane.ERROR_MESSAGE);
                return;
            }

            loadedNotClassifiedImages.clear();

            JVCFileParser parser = JVCFileParser.getParserInstance();

            try {
                Map<Path, LabeledImageModel> loadedClassifiedImages = parser.parse(filePath);
                if (classifiedImages.size() != 0) {
                    for (Map.Entry<Path, LabeledImageModel> entry : classifiedImages.entrySet()) {
                        Path path = entry.getKey();
                        LabeledImageModel model = entry.getValue();
                        if (loadedClassifiedImages.containsKey(path) &&
                                !model.toString().equals(loadedClassifiedImages.get(path).toString())) {
                            int answer = JOptionPane.showConfirmDialog(JLabelingSystem.this,
                                    provider.get("load_ds_image") + path.toFile().getName() + provider.get("load_ds_warning")
                                    , provider.get(WARNING), JOptionPane.YES_NO_OPTION);
                            modified = true;

                            if (answer == JOptionPane.YES_OPTION) {
                                classifiedImages.put(path, loadedClassifiedImages.get(path));
                            }
                        } else {
                            classifiedImages.put(path, model);
                        }
                    }
                }
                for (Map.Entry<Path, LabeledImageModel> entry : loadedClassifiedImages.entrySet()) {
                    if (!classifiedImages.containsKey(entry.getKey())) {
                        loadedNotClassifiedImages.put(entry.getKey(), entry.getValue());
                    }
                }


                File imageFile = new File(imagesByName.get(imagesInSelectedDirectory.getSelectedValue()).toString());
                LabeledImageModel imageModel = classifiedImages.get(imageFile.toPath());
                ((BoundingBoxPanel) boundingBoxPanel).setViewModels(imageModel.getViewModels());


                boundingBoxes.revalidate();
                boundingBoxes.repaint();

                boundingBoxPanel.revalidate();
                boundingBoxPanel.repaint();


            } catch (IOException | FileFormatException e1) {
                JOptionPane.showMessageDialog(JLabelingSystem.this, provider.get("error_reading")
                        + fileName.getAbsolutePath()
                        + ".", provider.get(ERROR), JOptionPane.ERROR_MESSAGE);
                return;
            }

            JOptionPane.showMessageDialog(JLabelingSystem.this, provider.get("labels_loaded"),
                    provider.get(INFORMATION), JOptionPane.INFORMATION_MESSAGE);
        }
    };


    private AbstractAction saveDataset = new AbstractAction(provider.get("save_dataset")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            saveDataset();
            modified = false;
        }
    };

    private void saveDataset() {

        if (imagesInSelectedDirectory.getModel().getSize() != classifiedImages.size()) {
            JOptionPane.showMessageDialog(JLabelingSystem.this, provider.get("save_information"),
                    provider.get(INFORMATION), JOptionPane.INFORMATION_MESSAGE);
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Path, LabeledImageModel> entry : classifiedImages.entrySet()) {
            LabeledImageModel data = entry.getValue();
            sb.append(data.toString());
        }

        JFileChooser saveDataset = new JFileChooser();

        if (saveDataset.showSaveDialog(JLabelingSystem.this) != JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(JLabelingSystem.this, provider.get("nothing_was_saved"),
                    provider.get(INFORMATION),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Path path = saveDataset.getSelectedFile().toPath();
        File file = new File(path.toString());


        String pathString = path.toString();
        if (!file.getName().contains(".")) {
            JOptionPane.showMessageDialog(JLabelingSystem.this,
                    provider.get("save_in_jvc"),
                    provider.get(INFORMATION), JOptionPane.INFORMATION_MESSAGE);
            path = Paths.get(pathString + ".jvc");
        } else {
            path = Paths.get(pathString.substring(0, pathString.indexOf(".")), ".jvc");
        }


        if (path.toFile().isFile() && path.toFile().exists()) {
            int answer = JOptionPane.showConfirmDialog(JLabelingSystem.this,
                    provider.get("file_already_exist"),
                    provider.get(WARNING), JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.NO_OPTION || answer == JOptionPane.CLOSED_OPTION) {
                return;
            }
        }

        try (BufferedWriter bw = Files.newBufferedWriter(path)) {
            bw.write(sb.toString());
        } catch (IOException e1) {
            JOptionPane.showMessageDialog(JLabelingSystem.this,
                    provider.get("error_reading"), provider.get(INFORMATION),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(JLabelingSystem.this,
                provider.get("dataset_saved"), provider.get(INFORMATION),
                JOptionPane.INFORMATION_MESSAGE);
    }


    private AbstractAction previousImage = new AbstractAction(provider.get("previous_image")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            int currentIndex = imagesInSelectedDirectory.getSelectedIndex();
            if (currentIndex - 1 >= 0) {
                imagesInSelectedDirectory.setSelectedIndex(--currentIndex);
            } else {
                imagesInSelectedDirectory.setSelectedIndex(imagesInSelectedDirectory.getModel().getSize() - 1);
            }
            imagesInSelectedDirectory.requestFocus();
        }
    };

    private AbstractAction retrainNetwork = new AbstractAction(provider.get("retrain")) {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {

            try {
                net.retrain(classifiedImages.values());
            } catch (RetrainNetworkException e) {
                JOptionPane.showMessageDialog(JLabelingSystem.this, e.getMessage(),
                        provider.get(INFORMATION), JOptionPane.INFORMATION_MESSAGE);
            }
        }
    };


    private AbstractAction nextImage = new AbstractAction(provider.get("next_image")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            int currentIndex = imagesInSelectedDirectory.getSelectedIndex();
            int maxIndex = imagesInSelectedDirectory.getModel().getSize() - 1;
            if (currentIndex + 1 <= maxIndex) {
                imagesInSelectedDirectory.setSelectedIndex(++currentIndex);
            } else {
                imagesInSelectedDirectory.setSelectedIndex(0);
            }
            imagesInSelectedDirectory.requestFocus();
        }
    };


    private AbstractAction addBoundingBoxAction = new AbstractAction(provider.get("add_bb")) {
        @Override
        public void actionPerformed(ActionEvent e) {

        }
    };


    private void addListenerToBoundingBoxesList() {

        boundingBoxes.addListSelectionListener(e -> {
            int selectedIndex = boundingBoxes.getSelectedIndex();
            if (selectedIndex != -1) {
                Path selectedImagePath = imagesByName.get(imagesInSelectedDirectory.getSelectedValue());
                BoundingBoxPanel panel = (BoundingBoxPanel) boundingBoxPanel;
                panel.setViewModels(classifiedImages.get(selectedImagePath).getViewModels());
                panel.setSelectedBox(selectedIndex);
                boundingBoxes.revalidate();
                boundingBoxes.repaint();

            }
            boundingBoxPanel.revalidate();
            boundingBoxPanel.repaint();
        });

        boundingBoxes.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editBoundingBox();
                }
            }
        });

        KeyAdapter adapter = new JListKeyNavigationListener<>(boundingBoxes);
        boundingBoxes.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    int selectedIndex = boundingBoxes.getSelectedIndex();
                    if (selectedIndex != -1) {

                        Path selectedImagePath = imagesByName.get(imagesInSelectedDirectory.getSelectedValue());
                        classifiedImages.get(selectedImagePath).getViewModels().remove(boundingBoxes.getSelectedValue());
                        ((DefaultListModel) boundingBoxes.getModel()).remove(selectedIndex);
                        ((BoundingBoxPanel) boundingBoxPanel).setSelectedBox(-1);
                        ((BoundingBoxPanel) boundingBoxPanel).clearMovedOverArea();

                        int size = boundingBoxes.getModel().getSize();
                        if (size != 0) {
                            if (selectedIndex < boundingBoxes.getModel().getSize()) {
                                boundingBoxes.setSelectedIndex(selectedIndex);
                            } else {
                                boundingBoxes.setSelectedIndex(0);
                            }
                        }
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    editBoundingBox();
                } else {
                    adapter.keyPressed(e);
                }
            }
        });
    }

    private void editBoundingBox() {
        BoxPredictionViewModel clicked = boundingBoxes.getSelectedValue();
        showEditor(clicked);
    }


    private void addListenerToList() {

        imagesInSelectedDirectory.addKeyListener(new JListKeyNavigationListener<>(imagesInSelectedDirectory));

        imagesInSelectedDirectory.addListSelectionListener(e -> {
            try {

                Set<BoxPredictionViewModel> viewModels = new TreeSet<>();
                List<BufferedImage> images;
                List<BoundingBox> boxes;
                BufferedImage image;

                if (imagesByName.size() == 0) {
                    return;
                }

                File imageFile = new File(imagesByName.get(imagesInSelectedDirectory
                        .getSelectedValue()).toString());

                if (loadedNotClassifiedImages.containsKey(imageFile.toPath())) {
                    this.classifiedImages.put(imageFile.toPath(), loadedNotClassifiedImages.get(imageFile.toPath()));
                    modified = true;
                }

                if (!this.classifiedImages.containsKey(imageFile.toPath())) {

                    LabeledImageModel imageModel = new LabeledImageModel(imageFile.toPath());
                    image = ImageIO.read(imageFile);

                    boxes = ImageManager.getBoundingBoxesAroundImage(image, new ConnectedComponent());
                    images = ImageManager.getImagesAroundBoundingBoxes(image, boxes);

                    int i = 0;
                    OptionalDouble average = boxes.stream().mapToInt(BoundingBox::getWidth).average();
                    double averageWidth = 0.0;
                    if (average.isPresent()) {
                        averageWidth = average.getAsDouble();
                    }

                    for (BufferedImage currentImage : images) {


                        List<Pair<BoundingBox, Integer>> pairs = ImageManager.postProcessImage(net, currentImage,
                                boxes.get(i), averageWidth);
                        if (pairs == null) {
                            int output = net.predictOutput(currentImage);
                            boxes.get(i).setGroupColor(RandomColorChooser.getColorForPrediction(output));
                            viewModels.add(new BoxPredictionViewModel(boxes.get(i), output));
                        } else {
                            for (Pair<BoundingBox, Integer> pair : pairs) {
                                pair.getKey().setGroupColor(RandomColorChooser.getColorForPrediction(pair.getValue()));
                                viewModels.add(new BoxPredictionViewModel(pair.getKey(), pair.getValue()));
                            }
                        }
                        i++;
                    }

                    imageModel.setViewModels(viewModels);
                    imageModel.setImage(image);
                    this.classifiedImages.put(imageFile.toPath(), imageModel);
                } else {
                    LabeledImageModel imageModel = this.classifiedImages.get(imageFile.toPath());
                    image = imageModel.getImage();
                    viewModels = imageModel.getViewModels();
                }


                DefaultListModel<BoxPredictionViewModel> boundingBoxes = new DefaultListModel<>();


                for (BoxPredictionViewModel model : viewModels) {
                    boundingBoxes.addElement(model);
                }

                this.boundingBoxes.setModel(boundingBoxes);


                ((ImagePanel) imagePanel).setImage(image);
                ((BoundingBoxPanel) boundingBoxPanel).setViewModels(viewModels);
                ((BoundingBoxPanel) boundingBoxPanel).setSelectedBox(-1);
                imagePanel.revalidate();
                imagePanel.repaint();

                this.boundingBoxes.revalidate();
                this.boundingBoxes.repaint();
                saveDataset.setEnabled(true);


            } catch (Exception ex) {
                //
                ex.printStackTrace();
            }

        });
    }


    @Override
    public void modelChanged(BoxPredictionViewModel model) {

        JPanel classificationPanel = new ClassificationPanel();
        int number;
        int answer = JOptionPane.showConfirmDialog(JLabelingSystem.this, classificationPanel,
                provider.get("classification_number"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (answer == JOptionPane.CANCEL_OPTION || answer == JOptionPane.CLOSED_OPTION) {
            BoundingBoxPanel panel = ((BoundingBoxPanel) boundingBoxPanel);
            panel.getViewModels().remove(model);
            panel.revalidate();
            panel.repaint();
            return;
        } else {
            number = ((ClassificationPanel) classificationPanel).getModelClassificationNumber();
        }

        model.setPrediction(number);
        model.getBoundingBox().setGroupColor(RandomColorChooser.getColorForPrediction(number));

        List<BoxPredictionViewModel> elements = Collections.list(((DefaultListModel<BoxPredictionViewModel>)
                boundingBoxes.getModel()).elements());


        List<BoxPredictionViewModel> boxesInsideNewBox = new ArrayList<>();
        for (BoxPredictionViewModel element : elements) {
            if (element.getBoundingBox().isInsideBox(model.getBoundingBox())) {
                boxesInsideNewBox.add(element);
            }
        }

        elements.removeAll(boxesInsideNewBox);
        elements.add(model);


        DefaultListModel<BoxPredictionViewModel> newModel = new DefaultListModel<>();
        int index = -1;

        int i = 0;
        Set<BoxPredictionViewModel> sortedViewModels = new TreeSet<>(elements);
        for (BoxPredictionViewModel viewModel : sortedViewModels) {
            newModel.addElement(viewModel);
            if (viewModel.equals(model)) {
                index = i;
            }
            i++;
        }
        boundingBoxes.setModel(newModel);

        File imageFile = new File(imagesByName.get(imagesInSelectedDirectory
                .getSelectedValue()).toString());

        LabeledImageModel imageModel = this.classifiedImages.get(imageFile.toPath());
        imageModel.setViewModels(sortedViewModels);

        boundingBoxes.setSelectedIndex(index);
        boundingBoxes.revalidate();
        boundingBoxes.repaint();
        boundingBoxes.requestFocus();
    }

    @Override
    public void selectedForEdit(BoxPredictionViewModel viewModel) {
        showEditor(viewModel);
    }

    private void showEditor(BoxPredictionViewModel viewModel) {
        File imageFile = new File(imagesByName.get(imagesInSelectedDirectory
                .getSelectedValue()).toString());

        LabeledImageModel imageModel = classifiedImages.get(imageFile.toPath());
        BufferedImage image = imageModel.getImage();
        GeometricalObjectEditor editor = new BoundingBoxEditPanel(boundingBoxPanel, image.getWidth(), image.getHeight(), viewModel);

        showEditDialog(editor);
    }

    @Override
    public void selected(BoxPredictionViewModel viewModel) {
        if (viewModel == null) {
            boundingBoxes.clearSelection();
            ((BoundingBoxPanel) boundingBoxPanel).setSelectedBox(-1);
            return;
        }
        boundingBoxes.setSelectedValue(viewModel, true);
        boundingBoxes.requestFocus();
    }

    @Override
    public void movedOver(BoxPredictionViewModel model) {
        BoundingBoxPanel panel = (BoundingBoxPanel) boundingBoxPanel;
        if (model != null) {
            panel.setMovedOver(model);
        } else {
            panel.clearMovedOverArea();
        }
        panel.revalidate();
        panel.repaint();
    }

    private void showEditDialog(GeometricalObjectEditor editor) {
        if (JOptionPane.showConfirmDialog(JLabelingSystem.this, editor,
                provider.get("edit"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {

                editor.acceptEditing();
                List<BoxPredictionViewModel> viewModels = Collections.list(((DefaultListModel<BoxPredictionViewModel>)
                        boundingBoxes.getModel()).elements());
                DefaultListModel<BoxPredictionViewModel> sortedModel = new DefaultListModel<>();
                Set<BoxPredictionViewModel> sortedElements = new TreeSet<>(viewModels);
                for (BoxPredictionViewModel model : sortedElements) {
                    sortedModel.addElement(model);
                }

                boundingBoxes.setModel(sortedModel);
                Path selectedImagePath = imagesByName.get(imagesInSelectedDirectory.getSelectedValue());
                classifiedImages.get(selectedImagePath).setViewModels(sortedElements);

                boundingBoxes.setSelectedIndex(-1);

                boundingBoxes.revalidate();
                boundingBoxes.repaint();

                boundingBoxPanel.revalidate();
                boundingBoxPanel.repaint();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(JLabelingSystem.this,
                        provider.get("invalid_arguments"));
            }
        } else {
            editor.cancelEditing();
        }
    }


    private synchronized void addWindowListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitAction();
            }
        });
    }

    private void exitAction() {
        if (modified) {
            int answer = JOptionPane.showConfirmDialog(JLabelingSystem.this,
                    provider.get("save_before_exit"), provider.get("save"),
                    JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.YES_OPTION) {
                saveDataset();
            }
        }
        dispose();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new JLabelingSystem().setVisible(true));
    }
}
