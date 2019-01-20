package hr.fer.zemris.projekt.gui;

import hr.fer.zemris.projekt.Classifier;
import hr.fer.zemris.projekt.exceptions.FileFormatException;
import hr.fer.zemris.projekt.gui.filters.ImageFilter;
import hr.fer.zemris.projekt.gui.icons.Icons;
import hr.fer.zemris.projekt.gui.listeners.IBoundingBoxModelChangeListener;
import hr.fer.zemris.projekt.gui.listeners.IBoundingBoxSelectListener;
import hr.fer.zemris.projekt.gui.listeners.IDrawingStatusListener;
import hr.fer.zemris.projekt.gui.listeners.JListKeyNavigationListener;
import hr.fer.zemris.projekt.gui.models.LabeledImageModel;
import hr.fer.zemris.projekt.gui.panels.*;
import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.image.utils.ImageUtils;
import hr.fer.zemris.projekt.parser.JVCFileParser;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;

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
import java.util.*;
import java.util.List;

import static hr.fer.zemris.projekt.gui.providers.MessageProvider.*;

public class JLabelingSystem extends JFrame implements IBoundingBoxModelChangeListener, IBoundingBoxSelectListener {


    private MultiLayerNetwork net;

    private JList<String> imagesInSelectedDirectory;
    private JList<BoundingBox> boundingBoxes;
    private List<IDrawingStatusListener> listeners;

    private JPanel imagePanel;
    private JPanel boundingBoxPanel;

    private JButton previousImageBtn;
    private JButton nextImageBtn;
    private JToggleButton addBoundingBox;

    private Map<String, Path> imagesByName;
    private Map<Path, LabeledImageModel> classifiedImages;
    private Map<Path, LabeledImageModel> loadedNotClassifiedImages;


    private JLayeredPane layeredPane;
    private boolean modified;


    public JLabelingSystem() {
        setLocation(0, 0);
        setSize(1200, 800);
        setTitle("Digits Labeling System");
        setLayout(new BorderLayout());


        try {
            net = ModelSerializer.restoreMultiLayerNetwork(new File(NETWORK_PATH));
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
    }


    private void initGui() {

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout());

        previousImageBtn = new JButton(previousImage);
        nextImageBtn = new JButton(nextImage);
        addBoundingBox = new JToggleButton(addBoundingBoxAction);
        addBoundingBox.setIcon(Icons.getInstance().getIcon("box"));
        addBoundingBox.setText("Add bounding box");

        previousImageBtn.setEnabled(false);
        nextImage.setEnabled(false);
        addBoundingBox.setEnabled(false);

        buttonsPanel.add(previousImageBtn);
        buttonsPanel.add(nextImageBtn);
        buttonsPanel.add(addBoundingBox);

        add(new JScrollPane(layeredPane), BorderLayout.CENTER);

        imagePanel.setOpaque(false);
        imagePanel.setBounds(0, 0, getWidth(), getHeight());

        boundingBoxPanel.setOpaque(false);
        boundingBoxPanel.setBounds(0, 0, getWidth(), getHeight());

        layeredPane.add(imagePanel, JLayeredPane.FRAME_CONTENT_LAYER);
        layeredPane.add(boundingBoxPanel, JLayeredPane.MODAL_LAYER);

        add(buttonsPanel, BorderLayout.PAGE_START);

        imagesInSelectedDirectory.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        boundingBoxes.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        addListenerToList();
        addListenerToBoundingBoxesList();

        layeredPane.addMouseMotionListener((BoundingBoxPanel) boundingBoxPanel);
        layeredPane.addMouseListener((BoundingBoxPanel) boundingBoxPanel);

        addBoundingBox.addItemListener(e -> {
            for (IDrawingStatusListener listener : listeners) {
                listener.statusChanged(e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new GridLayout(0, 2));
        listPanel.add(new JScrollPane(imagesInSelectedDirectory));
        listPanel.add(new JScrollPane(boundingBoxes));

        add(listPanel, BorderLayout.LINE_END);
    }


    private void initActions() {
        Icons icons = Icons.getInstance();
        nextImage.putValue(Action.SMALL_ICON, icons.getIcon("next"));
        previousImage.putValue(Action.SMALL_ICON, icons.getIcon("previous"));
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
        JOptionPane.showMessageDialog(JLabelingSystem.this, MESSAGE_NEURAL_NETWORK_ERROR,
                "Error", JOptionPane.ERROR_MESSAGE);
    }


    private AbstractAction exitAction = new AbstractAction("Exit") {
        @Override
        public void actionPerformed(ActionEvent e) {
            exitAction();
        }
    };

    private AbstractAction directoryChooser = new AbstractAction("Select directory") {
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean clear = false;
            if (modified) {
                int answer = JOptionPane.showConfirmDialog(JLabelingSystem.this,
                        MESSAGE_SAVE_BEFORE_CHANGE_DIRECTORY, "Save changes",
                        JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.YES_OPTION) {
                    saveDataset();
                }
                clear = true;
            }


            JFileChooser directoryChooser = new JFileChooser();
            directoryChooser.setDialogTitle(MESSAGE_SELECT_FOLDER_WITH_IMAGES);
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
            FilenameFilter imageFilter = new ImageFilter();
            if (directory.isDirectory()) {
                File[] images = directory.listFiles(imageFilter);
                for (File image : images) {
                    imagesInDirectoryModel.addElement(image.getName());
                    imagesByName.put(image.getName(), image.toPath());
                }
            }
            if (imagesByName.size() == 0) {
                JOptionPane.showMessageDialog(JLabelingSystem.this, MESSAGE_NO_IMAGES_IN_DIR,
                        INFORMATION, JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            loadDataset.setEnabled(true);
            modified = true;


            imagesInSelectedDirectory.setModel(imagesInDirectoryModel);
            if (imagesByName.size() > 0) {
                imagesInSelectedDirectory.setSelectedIndex(0);

                nextImageBtn.setEnabled(true);
                previousImageBtn.setEnabled(true);
                addBoundingBox.setEnabled(true);
            } else {
                nextImageBtn.setEnabled(false);
                previousImageBtn.setEnabled(false);
                addBoundingBox.setEnabled(false);

            }
            imagesInSelectedDirectory.revalidate();
            imagesInSelectedDirectory.repaint();
        }
    };

    private void clearAll() {
        classifiedImages.clear();
        imagesByName.clear();
        ((DefaultListModel<BoundingBox>) boundingBoxes.getModel()).removeAllElements();
        ((DefaultListModel<String>) imagesInSelectedDirectory.getModel()).removeAllElements();
    }


    private AbstractAction loadDataset = new AbstractAction("Load dataset") {
        @Override
        public void actionPerformed(ActionEvent e) {

            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Open JVC file");
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Image bounding box description" +
                    " file (JVC)", "jvc");
            fc.setAcceptAllFileFilterUsed(false);
            fc.setFileFilter(filter);

            if (fc.showOpenDialog(JLabelingSystem.this) != JFileChooser.APPROVE_OPTION) {
                return;
            }


            File fileName = fc.getSelectedFile();
            Path filePath = fileName.toPath();
            if (!Files.isReadable(filePath) || !fileName.toString().endsWith(".jvc")) {
                JOptionPane.showMessageDialog(JLabelingSystem.this, "The file : "
                        + fileName.getAbsolutePath()
                        + " can not be loaded.", "Error", JOptionPane.ERROR_MESSAGE);
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

                                    "The hand-labeled image " + path.toFile().getName() +
                                            " differs from labeling in the dataset.\n " +
                                            "Do you want to overwrite the current labels?"
                                    , WARNING, JOptionPane.YES_NO_OPTION);
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
                ((BoundingBoxPanel) boundingBoxPanel).setBoxes(imageModel.getBoundingBoxes());
                ((BoundingBoxPanel) boundingBoxPanel).setClassifications(imageModel.getClassifications());


                boundingBoxes.revalidate();
                boundingBoxes.repaint();

                boundingBoxPanel.revalidate();
                boundingBoxPanel.repaint();


            } catch (IOException | FileFormatException e1) {
                JOptionPane.showMessageDialog(JLabelingSystem.this, MESSAGE_ERROR_READING
                        + fileName.getAbsolutePath()
                        + ".", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JOptionPane.showMessageDialog(JLabelingSystem.this, MESSAGE_LABELS_LOADED,
                    INFORMATION, JOptionPane.INFORMATION_MESSAGE);
        }
    };


    private AbstractAction saveDataset = new AbstractAction("Save dataset") {
        @Override
        public void actionPerformed(ActionEvent e) {
            saveDataset();
            modified = false;
        }
    };

    private void saveDataset() {

        if (imagesInSelectedDirectory.getModel().getSize() != classifiedImages.size()) {
            JOptionPane.showMessageDialog(JLabelingSystem.this, MESSAGE_SAVE_INFORMATION,
                    INFORMATION, JOptionPane.INFORMATION_MESSAGE);
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Path, LabeledImageModel> entry : classifiedImages.entrySet()) {
            LabeledImageModel data = entry.getValue();
            sb.append(data.toString());
        }

        JFileChooser saveDataset = new JFileChooser();

        if (saveDataset.showSaveDialog(JLabelingSystem.this) != JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(JLabelingSystem.this, MESSAGE_NOTHING_WAS_SAVED,
                    INFORMATION,
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Path path = saveDataset.getSelectedFile().toPath();
        File file = new File(path.toString());


        String pathString = path.toString();
        if (!file.getName().contains(".")) {
            JOptionPane.showMessageDialog(JLabelingSystem.this,
                    MESSAGE_SAVE_IN_JVC_FORMAT,
                    INFORMATION, JOptionPane.INFORMATION_MESSAGE);
            path = Paths.get(pathString.substring(0, pathString.length()), ".jvc");
        } else {
            path = Paths.get(pathString.substring(0, pathString.indexOf(".")), ".jvc");
        }


        if (path.toFile().isFile() && path.toFile().exists()) {
            int answer = JOptionPane.showConfirmDialog(JLabelingSystem.this,
                    MESSAGE_FILE_ALREADY_EXIST,
                    WARNING, JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (answer == JOptionPane.NO_OPTION || answer == JOptionPane.CLOSED_OPTION) {
                return;
            }
        }

        try (BufferedWriter bw = Files.newBufferedWriter(path)) {
            bw.write(sb.toString());
        } catch (IOException e1) {
            JOptionPane.showMessageDialog(JLabelingSystem.this,
                    MESSAGE_ERROR_READING, INFORMATION,
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(JLabelingSystem.this,
                MESSAGE_SAVED, INFORMATION,
                JOptionPane.INFORMATION_MESSAGE);
    }


    private AbstractAction previousImage = new AbstractAction("Previous image") {
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


    private AbstractAction nextImage = new AbstractAction("Next image") {
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


    private AbstractAction addBoundingBoxAction = new AbstractAction("Dodaj novi okvir") {
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
                panel.setBoxes(classifiedImages.get(selectedImagePath).getBoundingBoxes());
                panel.setClassifications(classifiedImages.get(selectedImagePath).getClassifications());
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
                        ((DefaultListModel) boundingBoxes.getModel()).remove(selectedIndex);

                        classifiedImages.get(selectedImagePath).getBoundingBoxes().remove(selectedIndex);
                        classifiedImages.get(selectedImagePath).getClassifications().remove(selectedIndex);
                        ((BoundingBoxPanel) boundingBoxPanel).setSelectedBox(-1);
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
        BoundingBox clicked = boundingBoxes.getSelectedValue();
        File imageFile = new File(imagesByName.get(imagesInSelectedDirectory
                .getSelectedValue()).toString());
        int index = boundingBoxes.getSelectedIndex();

        LabeledImageModel imageModel = classifiedImages.get(imageFile.toPath());
        BufferedImage image = imageModel.getImage();
        GeometricalObjectEditor editor = new BoundingBoxEditPanel(image.getWidth(), image.getHeight(),
                clicked, imageModel.getClassifications(), index);

        showEditDialog(editor);
    }

    private void addListenerToList() {

        imagesInSelectedDirectory.addKeyListener(new JListKeyNavigationListener<>(imagesInSelectedDirectory));

        imagesInSelectedDirectory.addListSelectionListener(e -> {
            try {
                List<BoundingBox> boxes;
                List<BufferedImage> images;
                List<Integer> classifications;
                BufferedImage image;

                File imageFile = new File(imagesByName.get(imagesInSelectedDirectory
                        .getSelectedValue()).toString());

                if (loadedNotClassifiedImages.containsKey(imageFile.toPath())) {
                    this.classifiedImages.put(imageFile.toPath(), loadedNotClassifiedImages.get(imageFile.toPath()));
                    modified = true;
                }

                if (!this.classifiedImages.containsKey(imageFile.toPath())) {
                    LabeledImageModel imageModel = new LabeledImageModel(imageFile.toPath());
                    image = ImageIO.read(imageFile);
                    boxes = ImageUtils.getBoundingBoxesAroundImage(image);

                    images = ImageUtils.getImagesAroundBoundingBoxes(image, boxes);
                    Classifier classifier = Classifier.getInstance();
                    classifications = new ArrayList<>();
                    for (BufferedImage currentImage : images) {
                        classifications.add(classifier.classify(net, currentImage));
                    }

                    imageModel.setBoundingBoxes(boxes);
                    imageModel.setClassifications(classifications);
                    imageModel.setImage(image);
                    this.classifiedImages.put(imageFile.toPath(), imageModel);
                } else {
                    LabeledImageModel imageModel = this.classifiedImages.get(imageFile.toPath());
                    image = imageModel.getImage();
                    boxes = imageModel.getBoundingBoxes();
                    classifications = imageModel.getClassifications();
                }


                DefaultListModel<BoundingBox> boundingBoxes = new DefaultListModel<>();
                for (BoundingBox box : boxes) {
                    boundingBoxes.addElement(box);
                }
                this.boundingBoxes.setModel(boundingBoxes);


                ((ImagePanel) imagePanel).setImage(image);
                ((BoundingBoxPanel) boundingBoxPanel).setBoxes(boxes);
                ((BoundingBoxPanel) boundingBoxPanel).setClassifications(classifications);
                ((BoundingBoxPanel) boundingBoxPanel).setSelectedBox(-1);
                imagePanel.revalidate();
                imagePanel.repaint();

                boundingBoxPanel.setMaximumSize(new Dimension(image.getWidth(), image.getHeight()));

                this.boundingBoxes.revalidate();
                this.boundingBoxes.repaint();
                saveDataset.setEnabled(true);


            } catch (Exception ex) {
                //
            }

        });
    }


    @Override
    public void modelChanged(BoundingBox newBox) {

        JPanel classificationPanel = new ClassificationPanel();
        int number;
        int answer = JOptionPane.showConfirmDialog(JLabelingSystem.this, classificationPanel,
                "Classification number",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (answer == JOptionPane.CANCEL_OPTION || answer == JOptionPane.CLOSED_OPTION) {
            BoundingBoxPanel panel = ((BoundingBoxPanel) boundingBoxPanel);
            int size = panel.getBoxes().size();
            panel.getBoxes().remove(size - 1);
            panel.revalidate();
            panel.repaint();
            return;
        } else {
            number = ((ClassificationPanel) classificationPanel).getModelClassificationNumber();
        }

        List<BoundingBox> elements = Collections.list(((DefaultListModel<BoundingBox>)
                boundingBoxes.getModel()).elements());
        elements.add(newBox);
        Collections.sort(elements);
        DefaultListModel<BoundingBox> model = new DefaultListModel<>();
        int index = -1;

        int i = 0;
        for (BoundingBox box : elements) {
            model.addElement(box);
            if (box == newBox) {
                index = i;
            }
            i++;
        }

        int indexAfterSort = elements.indexOf(newBox);

        boundingBoxes.setModel(model);

        File imageFile = new File(imagesByName.get(imagesInSelectedDirectory
                .getSelectedValue()).toString());

        LabeledImageModel imageModel = this.classifiedImages.get(imageFile.toPath());
        imageModel.setBoundingBoxes(elements);
        imageModel.getClassifications().add(indexAfterSort, number);

        boundingBoxes.setSelectedIndex(index);
        boundingBoxes.revalidate();
        boundingBoxes.repaint();
        boundingBoxes.requestFocus();
    }

    @Override
    public void selectedForEdit(int index) {
        BoundingBox clicked = boundingBoxes.getModel().getElementAt(index);
        boundingBoxes.setSelectedIndex(index);

        File imageFile = new File(imagesByName.get(imagesInSelectedDirectory
                .getSelectedValue()).toString());


        LabeledImageModel imageModel = classifiedImages.get(imageFile.toPath());
        BufferedImage image = imageModel.getImage();
        GeometricalObjectEditor editor = new BoundingBoxEditPanel(image.getWidth(), image.getHeight(), clicked,
                imageModel.getClassifications(), index);

        showEditDialog(editor);
    }

    @Override
    public void selected(int index) {
        if (index == -1) {
            boundingBoxes.clearSelection();
            ((BoundingBoxPanel) boundingBoxPanel).setSelectedBox(-1);
            return;
        }
        boundingBoxes.setSelectedIndex(index);
        boundingBoxes.requestFocus();
    }

    private void showEditDialog(GeometricalObjectEditor editor) {
        if (JOptionPane.showConfirmDialog(JLabelingSystem.this, editor,
                "Edit", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                editor.acceptEditing();
                boundingBoxPanel.revalidate();
                boundingBoxPanel.repaint();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(JLabelingSystem.this,
                        "Invalid arguments");
            }
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
                    MESSAGE_SAVE_BEFORE_EXIT, "Save",
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
