package hr.fer.zemris.projekt.gui.actions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

import static hr.fer.zemris.projekt.gui.JLabelingSystem.modified;
import static hr.fer.zemris.projekt.gui.JLabelingSystem.provider;


public class ExitAction extends AbstractAction {

    private Component parent;

    public ExitAction(String name, Component parent) {
        super(name);
        this.parent = parent;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        exitAction(parent);
    }

    public static void exitAction(Component parent) {
        if (modified) {
            int answer = JOptionPane.showConfirmDialog(parent,
                    provider.get("save_before_exit"), provider.get("save"),
                    JOptionPane.YES_NO_OPTION);
            if (answer == JOptionPane.YES_OPTION) {
                DatasetActions.saveDataset(parent);
            }
        }
        ((JFrame) parent).dispose();
    }
}
