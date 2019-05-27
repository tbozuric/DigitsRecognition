package hr.fer.zemris.projekt.gui.panels;

import hr.fer.zemris.projekt.gui.models.BoxPredictionViewModel;
import hr.fer.zemris.projekt.gui.services.RandomColorChooser;
import hr.fer.zemris.projekt.image.models.Point;

import javax.swing.*;
import java.awt.*;

public class BoundingBoxEditPanel extends GeometricalObjectEditor {

    private BoxPredictionViewModel viewModel;
    private BoxPredictionViewModel copyOfViewModel;
    private JComponent parent;

    private SpinnerNumberModel modelStartX;
    private SpinnerNumberModel modelStartY;
    private SpinnerNumberModel modelWidth;
    private SpinnerNumberModel modelHeight;
    private SpinnerNumberModel modelClassificationNumber;

    public BoundingBoxEditPanel(JComponent parent, int maxWidth, int maxHeight, BoxPredictionViewModel viewModel) {
        super(maxWidth, maxHeight);
        this.parent = parent;
        this.viewModel = viewModel;
        this.copyOfViewModel = viewModel.deepCopy();
        constructDialog();
    }

    private void constructDialog() {
        setLayout(new GridLayout(0, 2));
        Point upLeft = viewModel.getBoundingBox().getUpLeft();
        int x = upLeft.getX();
        int y = upLeft.getY();
        int width = viewModel.getBoundingBox().getWidth();
        int height = viewModel.getBoundingBox().getHeight();


        //effectively we can make the coordinates free, without limitation
        modelStartX = new SpinnerNumberModel(x, 0, maxWidth, 1);
        modelStartY = new SpinnerNumberModel(y, 0, maxHeight, 1);
        modelWidth = new SpinnerNumberModel(width, 0,
                maxWidth - x, 1);
        modelHeight = new SpinnerNumberModel(height, 0,
                maxHeight - y, 1);

        modelClassificationNumber = new SpinnerNumberModel(viewModel.getPrediction(),
                0, 9, 1);


        addChangeListener(modelStartX);
        addChangeListener(modelStartY);
        addChangeListener(modelHeight);
        addChangeListener(modelWidth);

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
        viewModel.getBoundingBox().setUpLeft(Point.create(modelStartX.getNumber().intValue(), modelStartY.getNumber().intValue()));
        viewModel.getBoundingBox().setWidth(modelWidth.getNumber().intValue());
        viewModel.getBoundingBox().setHeight(modelHeight.getNumber().intValue());
        viewModel.getBoundingBox().setGroupColor(RandomColorChooser.getColorForPrediction(modelClassificationNumber.getNumber().intValue()));
        viewModel.setPrediction(modelClassificationNumber.getNumber().intValue());

        parent.revalidate();
        parent.repaint();
    }

    @Override
    public void cancelEditing() {
        viewModel.getBoundingBox().setUpLeft(Point.create(copyOfViewModel.getBoundingBox().getUpLeft().getX(),
                copyOfViewModel.getBoundingBox().getUpLeft().getY()));
        viewModel.getBoundingBox().setWidth(copyOfViewModel.getBoundingBox().getWidth());
        viewModel.getBoundingBox().setHeight(copyOfViewModel.getBoundingBox().getHeight());

        viewModel.setPrediction(copyOfViewModel.getPrediction());
        parent.revalidate();
        parent.repaint();

    }

    private void addChangeListener(SpinnerNumberModel model) {
        model.addChangeListener(e -> acceptEditing());
    }
}
