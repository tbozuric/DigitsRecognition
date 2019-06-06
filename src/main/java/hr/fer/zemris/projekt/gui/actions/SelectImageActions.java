package hr.fer.zemris.projekt.gui.actions;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static hr.fer.zemris.projekt.gui.JLabelingSystem.imagesInSelectedDirectory;

public class SelectImageActions {

    public static class NextImage extends AbstractAction {

        public NextImage(String name) {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            int currentIndex = imagesInSelectedDirectory.getSelectedIndex();
            int maxIndex = imagesInSelectedDirectory.getModel().getSize() - 1;
            if (currentIndex + 1 <= maxIndex) {
                imagesInSelectedDirectory.setSelectedIndex(++currentIndex);
            } else {
                imagesInSelectedDirectory.setSelectedIndex(0);
            }
            imagesInSelectedDirectory.requestFocus();
        }
    }


    public static class PreviousImage extends AbstractAction {

        public PreviousImage(String name) {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            int currentIndex = imagesInSelectedDirectory.getSelectedIndex();
            if (currentIndex - 1 >= 0) {
                imagesInSelectedDirectory.setSelectedIndex(--currentIndex);
            } else {
                imagesInSelectedDirectory.setSelectedIndex(imagesInSelectedDirectory.getModel().getSize() - 1);
            }
            imagesInSelectedDirectory.requestFocus();
        }
    }
}
