package hr.fer.zemris.projekt.gui.listeners;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class JListKeyNavigationListener<T> extends KeyAdapter {

    private JList<T> list;

    public JListKeyNavigationListener(JList<T> list) {
        this.list = list;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_PLUS || e.getKeyCode() == KeyEvent.VK_ADD) {
            int selectedIndex = list.getSelectedIndex();
            int maxIndex = list.getModel().getSize() - 1;
            if (selectedIndex != -1) {
                if (selectedIndex + 1 <= maxIndex) {
                    list.setSelectedIndex(++selectedIndex);
                } else {
                    list.setSelectedIndex(0);
                }
            }

        } else if (e.getKeyCode() == KeyEvent.VK_MINUS || e.getKeyCode() == KeyEvent.VK_SUBTRACT) {
            int selectedIndex = list.getSelectedIndex();
            if (selectedIndex != -1) {
                if (selectedIndex - 1 >= 0) {
                    list.setSelectedIndex(--selectedIndex);
                } else {
                    list.setSelectedIndex(list.getModel().getSize() - 1);
                }
            }
        }
    }
}
