package hr.fer.zemris.projekt.gui.actions;

import hr.fer.zemris.projekt.gui.filters.ImageFilter;
import hr.fer.zemris.projekt.gui.listeners.IDetectionsModified;
import hr.fer.zemris.projekt.gui.models.BoxPredictionViewModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Set;
import java.util.TreeSet;

import static hr.fer.zemris.projekt.gui.JLabelingSystem.*;

public class DirectoryChooserAction extends AbstractAction {

    private Component parent;

    public DirectoryChooserAction(String name, Component parent) {
        super(name);
        this.parent = parent;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        boolean clear = false;
        if (modified) {
            int answer = JOptionPane.showConfirmDialog(parent,
                    provider.get("save_before_change_directory"), provider.get("save_changes"),
                    JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.YES_OPTION) {
                DatasetActions.saveDataset(parent);
            }
            clear = true;
        }


        JFileChooser directoryChooser = new JFileChooser();
        directoryChooser.setDialogTitle(provider.get("select_folder_with_images"));
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        directoryChooser.setAcceptAllFileFilterUsed(false);

        if (directoryChooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
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
            JOptionPane.showMessageDialog(parent, provider.get("no_images_in_dir"),
                    provider.get(INFORMATION), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        for (String image : imagesSet) {
            imagesInDirectoryModel.addElement(image);
        }

        loadDataset.setEnabled(true);

        ((IDetectionsModified) parent).modified(true);


        imagesInSelectedDirectory.setModel(imagesInDirectoryModel);
        if (imagesByName.size() > 0) {
            imagesInSelectedDirectory.setSelectedIndex(0);
            ((IDetectionsModified) parent).enableActions();
        } else {
            ((IDetectionsModified) parent).disableActions();
        }
        imagesInSelectedDirectory.revalidate();
        imagesInSelectedDirectory.repaint();
    }

    private void clearAll() {
        classifiedImages.clear();
        imagesByName.clear();
        ((DefaultListModel<BoxPredictionViewModel>) boundingBoxes.getModel()).removeAllElements();
        ((DefaultListModel<String>) imagesInSelectedDirectory.getModel()).removeAllElements();
    }

}
