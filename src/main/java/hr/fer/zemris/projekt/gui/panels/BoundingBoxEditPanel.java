package hr.fer.zemris.projekt.gui.panels;

import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.image.models.Point;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class BoundingBoxEditPanel extends GeometricalObjectEditor {

    private int index;
    private BoundingBox boundingBox;
    private List<Integer> classifications;

    private SpinnerNumberModel modelStartX;
    private SpinnerNumberModel modelStartY;
    private SpinnerNumberModel modelWidth;
    private SpinnerNumberModel modelHeight;
    private SpinnerNumberModel modelClassificationNumber;

    public BoundingBoxEditPanel(int maxWidth, int maxHeight, BoundingBox boundingBox, List<Integer> classifications,
                                int index) {
        super(maxWidth, maxHeight);
        this.boundingBox = boundingBox;
        this.index = index;
        this.classifications = classifications;
        this.index = index;
        constructDialog();
    }

    private void constructDialog() {
        setLayout(new GridLayout(0, 2));
        Point upLeft = boundingBox.getUpLeft();
        int x = upLeft.getX();
        int y = upLeft.getY();
        int width = boundingBox.getWidth();
        int height = boundingBox.getHeight();


        //effectively we can make the coordinates free, without limitation
        modelStartX = new SpinnerNumberModel(x, 0, maxWidth, 1);
        modelStartY = new SpinnerNumberModel(y, 0, maxHeight, 1);
        modelWidth = new SpinnerNumberModel(width, 0,
                maxWidth - x, 1);
        modelHeight = new SpinnerNumberModel(height, 0,
                maxHeight - y, 1);

        modelClassificationNumber = new SpinnerNumberModel((int) classifications.get(index),
                0, 9, 1);

        add(new JLabel("Change start x coordinate"));
        add(new JSpinner(modelStartX));
        add(new JLabel("Change start y coordinate"));
        add(new JSpinner(modelStartY));

        add(new JLabel("Change width "));
        add(new JSpinner(modelWidth));
        add(new JLabel("Change height"));
        add(new JSpinner(modelHeight));
        add(new JLabel("Change classification number "));
        add(new JSpinner(modelClassificationNumber));
    }

    @Override
    public void acceptEditing() {
        boundingBox.setUpLeft(new Point(modelStartX.getNumber().intValue(), modelStartY.getNumber().intValue()));
        boundingBox.setWidth(modelWidth.getNumber().intValue());
        boundingBox.setHeight(modelHeight.getNumber().intValue());
        classifications.set(index, modelClassificationNumber.getNumber().intValue());
    }
}
