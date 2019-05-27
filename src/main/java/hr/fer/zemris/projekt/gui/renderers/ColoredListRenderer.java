package hr.fer.zemris.projekt.gui.renderers;

import hr.fer.zemris.projekt.gui.models.BoxPredictionViewModel;

import javax.swing.*;
import java.awt.*;

public class ColoredListRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        BoxPredictionViewModel box = (BoxPredictionViewModel) value;
        if (isSelected) {
            c.setBackground(Color.WHITE);
        } else {
            c.setBackground(box.getBoundingBox().getGroupColor());
        }
        return c;
    }

}
