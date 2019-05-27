package hr.fer.zemris.projekt.gui.panels;

import javax.swing.*;

public abstract class GeometricalObjectEditor extends JPanel {

    /**
     * The default serial version UID.
     */
    private static final long serialVersionUID = -69260126098089997L;


    int maxWidth;
    int maxHeight;

    public GeometricalObjectEditor(int maxWidth, int maxHeight) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    public abstract void acceptEditing();

    public abstract void cancelEditing();
}