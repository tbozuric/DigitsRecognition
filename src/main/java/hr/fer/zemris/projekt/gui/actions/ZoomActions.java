package hr.fer.zemris.projekt.gui.actions;

import hr.fer.zemris.projekt.gui.panels.ZoomablePanel;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static hr.fer.zemris.projekt.gui.JLabelingSystem.boundingBoxPanel;
import static hr.fer.zemris.projekt.gui.JLabelingSystem.imagePanel;

public class ZoomActions {


    public static class ZoomIn extends AbstractAction {

        public ZoomIn(String name) {
            super(name);
        }


        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            ((ZoomablePanel) imagePanel).zoomIn();
            ((ZoomablePanel) boundingBoxPanel).zoomIn();
        }
    }

    public static class ZoomOut extends AbstractAction {

        public ZoomOut(String name) {
            super(name);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            ((ZoomablePanel) imagePanel).zoomOut();
            ((ZoomablePanel) boundingBoxPanel).zoomOut();
        }
    }
}
