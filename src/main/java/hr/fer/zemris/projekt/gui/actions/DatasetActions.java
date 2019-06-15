package hr.fer.zemris.projekt.gui.actions;

import hr.fer.zemris.projekt.exceptions.FileFormatException;
import hr.fer.zemris.projekt.gui.listeners.IDetectionsModified;
import hr.fer.zemris.projekt.gui.models.LabeledImageModel;
import hr.fer.zemris.projekt.gui.panels.BoundingBoxPanel;
import hr.fer.zemris.projekt.parser.JVCFileParser;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static hr.fer.zemris.projekt.gui.JLabelingSystem.*;


public class DatasetActions {

    public static class LoadDataset extends AbstractAction {

        private Component parent;

        public LoadDataset(String name, Component parent) {
            super(name);
            this.parent = parent;

        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {

            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle(provider.get("open_jvc_file"));
            FileNameExtensionFilter filter = new FileNameExtensionFilter(provider.get("bb_description_file"), "jvc");
            fc.setAcceptAllFileFilterUsed(false);
            fc.setFileFilter(filter);

            if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
                return;
            }


            File fileName = fc.getSelectedFile();
            Path filePath = fileName.toPath();
            if (!Files.isReadable(filePath) || !fileName.toString().endsWith(".jvc")) {
                JOptionPane.showMessageDialog(parent, provider.get("the_file")
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
                            int answer = JOptionPane.showConfirmDialog(parent,
                                    provider.get("load_ds_image") + path.toFile().getName() + provider.get("load_ds_warning")
                                    , provider.get(WARNING), JOptionPane.YES_NO_OPTION);
                            ((IDetectionsModified) parent).modified(true);

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
                JOptionPane.showMessageDialog(parent, provider.get("error_reading")
                        + fileName.getAbsolutePath()
                        + ".", provider.get(ERROR), JOptionPane.ERROR_MESSAGE);
                return;
            }

            JOptionPane.showMessageDialog(parent, provider.get("labels_loaded"),
                    provider.get(INFORMATION), JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static class SaveDataset extends AbstractAction {
        private Component parent;

        public SaveDataset(String name, Component parent) {
            super(name);
            this.parent = parent;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            saveDataset(parent);
            ((IDetectionsModified) parent).modified(false);
        }
    }

    static void saveDataset(Component parent) {

        if (imagesInSelectedDirectory.getModel().getSize() != classifiedImages.size()) {
            JOptionPane.showMessageDialog(parent, provider.get("save_information"),
                    provider.get(INFORMATION), JOptionPane.INFORMATION_MESSAGE);
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Path, LabeledImageModel> entry : classifiedImages.entrySet()) {
            LabeledImageModel data = entry.getValue();
            sb.append(data.toString());
        }

        JFileChooser saveDataset = new JFileChooser();

        if (saveDataset.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(parent, provider.get("nothing_was_saved"),
                    provider.get(INFORMATION),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Path path = saveDataset.getSelectedFile().toPath();
        File file = new File(path.toString());


        String pathString = path.toString();
        if (!file.getName().contains(".")) {
            JOptionPane.showMessageDialog(parent,
                    provider.get("save_in_jvc"),
                    provider.get(INFORMATION), JOptionPane.INFORMATION_MESSAGE);
            path = Paths.get(pathString + ".jvc");
        } else {
            path = Paths.get(pathString.substring(0, pathString.indexOf(".")), ".jvc");
        }


        if (path.toFile().isFile() && path.toFile().exists()) {
            int answer = JOptionPane.showConfirmDialog(parent,
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
            JOptionPane.showMessageDialog(parent,
                    provider.get("error_reading"), provider.get(INFORMATION),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(parent,
                provider.get("dataset_saved"), provider.get(INFORMATION),
                JOptionPane.INFORMATION_MESSAGE);
    }
}
