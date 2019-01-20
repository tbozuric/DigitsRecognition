package hr.fer.zemris.projekt.gui.panels;

import javax.swing.*;

public class ClassificationPanel extends JPanel {
    private SpinnerNumberModel modelClassificationNumber;


    public ClassificationPanel(){
        modelClassificationNumber = new SpinnerNumberModel(0, 0, 9 ,1);
        add(new JLabel("Set classification number "));
        add(new JSpinner(modelClassificationNumber));
    }

    public int getModelClassificationNumber() {
        return modelClassificationNumber.getNumber().intValue();
    }
}
