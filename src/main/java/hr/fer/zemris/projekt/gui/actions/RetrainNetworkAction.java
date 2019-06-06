package hr.fer.zemris.projekt.gui.actions;

import hr.fer.zemris.projekt.neural.exceptions.RetrainNetworkException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

import static hr.fer.zemris.projekt.gui.JLabelingSystem.*;

public class RetrainNetworkAction extends AbstractAction {

    private Component parent;

    public RetrainNetworkAction(String name, Component parent) {
        super(name);
        this.parent = parent;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        try {
            net.retrain(classifiedImages.values());
        } catch (RetrainNetworkException e) {
            JOptionPane.showMessageDialog(parent, e.getMessage(),
                    provider.get(INFORMATION), JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
