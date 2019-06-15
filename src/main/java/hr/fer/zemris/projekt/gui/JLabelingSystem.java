package hr.fer.zemris.projekt.gui;

import hr.fer.zemris.projekt.gui.actions.*;
import hr.fer.zemris.projekt.gui.icons.Icons;
import hr.fer.zemris.projekt.gui.listeners.*;
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
import org.apache.commons.math3.util.Pair;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

public class JLabelingSystem extends JFrame implements IBoundingBoxModelChangeListener, IBoundingBoxActionListener,
        IDetectionsModified {

    private static final String NETWORK_PATH = "src/main/resources/digits-model_extended_6.zip";

    public static final String ERROR = "error";
    public static final String WARNING = "warning";
    public static final String INFORMATION = "information";


    public static final MessageProvider provider = MessageProvider.getInstance();

    public static JList<String> imagesInSelectedDirectory;
    public static JList<BoxPredictionViewModel> boundingBoxes;
    public static List<IDrawingStatusListener> listeners;

    public static JPanel imagePanel;
    public static JPanel boundingBoxPanel;

    public static Map<String, Path> imagesByName;
    public static Map<Path, LabeledImageModel> classifiedImages;
    public static Map<Path, LabeledImageModel> loadedNotClassifiedImages;
    public static boolean modified;

    public static INetwork net;

    public static AbstractAction loadDataset;
    private JToggleButton addBoundingBoxBtn;
    private AbstractAction zoomIn;
    private AbstractAction zoomOut;
    private AbstractAction saveDataset;
    private AbstractAction directoryChooser;
    private AbstractAction nextImage;
    private AbstractAction previousImage;
    private AbstractAction retrainNetwork;
    private AbstractAction exitAction;
    private AbstractAction addBoundingBoxAction;

    private JLayeredPane layeredPane;

    private JLabelingSystem() {
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


        zoomIn = new ZoomActions.ZoomIn(provider.get("zoom_in"));
        zoomOut = new ZoomActions.ZoomOut(provider.get("zoom_out"));


        loadDataset = new DatasetActions.LoadDataset(provider.get("load_dataset"), JLabelingSystem.this);
        saveDataset = new DatasetActions.SaveDataset(provider.get("save_dataset"), JLabelingSystem.this);
        directoryChooser = new DirectoryChooserAction(provider.get("select_directory"), JLabelingSystem.this);
        nextImage = new SelectImageActions.NextImage(provider.get("next_image"));
        previousImage = new SelectImageActions.PreviousImage(provider.get("previous_image"));
        retrainNetwork = new RetrainNetworkAction(provider.get("retrain"), JLabelingSystem.this);
        exitAction = new ExitAction(provider.get("exit"), JLabelingSystem.this);
        addBoundingBoxAction = new AddBoundingBoxAction(provider.get("add_bb"));

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


    @Override
    public void disableActions() {
        nextImage.setEnabled(false);
        previousImage.setEnabled(false);
        addBoundingBoxBtn.setEnabled(false);
        retrainNetwork.setEnabled(false);
        zoomIn.setEnabled(false);
        zoomOut.setEnabled(false);
    }

    @Override
    public void enableActions() {
        nextImage.setEnabled(true);
        previousImage.setEnabled(true);
        addBoundingBoxBtn.setEnabled(true);
        retrainNetwork.setEnabled(true);
        zoomIn.setEnabled(true);
        zoomOut.setEnabled(true);
    }


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
                    classifiedImages.put(imageFile.toPath(), loadedNotClassifiedImages.get(imageFile.toPath()));
                    modified = true;
                }

                if (!classifiedImages.containsKey(imageFile.toPath())) {

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
                    classifiedImages.put(imageFile.toPath(), imageModel);
                } else {
                    LabeledImageModel imageModel = classifiedImages.get(imageFile.toPath());
                    image = imageModel.getImage();
                    viewModels = imageModel.getViewModels();
                }


                DefaultListModel<BoxPredictionViewModel> boundingBoxesModel = new DefaultListModel<>();


                for (BoxPredictionViewModel model : viewModels) {
                    boundingBoxesModel.addElement(model);
                }

                boundingBoxes.setModel(boundingBoxesModel);


                ((ImagePanel) imagePanel).setImage(image);
                ((BoundingBoxPanel) boundingBoxPanel).setViewModels(viewModels);
                ((BoundingBoxPanel) boundingBoxPanel).setSelectedBox(-1);
                imagePanel.revalidate();
                imagePanel.repaint();

                boundingBoxes.revalidate();
                boundingBoxes.repaint();
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

        LabeledImageModel imageModel = classifiedImages.get(imageFile.toPath());
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

    @Override
    public void modified(boolean modifiedValue) {
        modified = modifiedValue;
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
                ExitAction.exitAction(JLabelingSystem.this);
            }
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new JLabelingSystem().setVisible(true));
    }


}
