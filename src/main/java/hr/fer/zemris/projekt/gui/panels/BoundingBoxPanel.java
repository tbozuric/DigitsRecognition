package hr.fer.zemris.projekt.gui.panels;

import hr.fer.zemris.projekt.gui.listeners.IBoundingBoxActionListener;
import hr.fer.zemris.projekt.gui.listeners.IBoundingBoxModelChangeListener;
import hr.fer.zemris.projekt.gui.listeners.IDrawingStatusListener;
import hr.fer.zemris.projekt.gui.models.BoxPredictionViewModel;
import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.image.models.Line;
import hr.fer.zemris.projekt.image.models.Point;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class BoundingBoxPanel extends ZoomablePanel implements MouseMotionListener, MouseListener, IDrawingStatusListener {

    //private static final int CLASSIFICATION_BOX_HEIGHT = 20;
    private static final double DISTANCE_TO_RECTANGLE = 0.5;
    private static final int CLASSIFICATION_BOX_WIDTH = 16;
    private static final int GAP_BOUNDING_CLASSIFICATION_BOX = 2;
    private static final int TEXT_START_X = 3;
    private static final int TEXT_START_Y = 10;
    private static final Color movedOverColor = new Color(102, 0, 255, 50);

    private boolean drawingEnabled = false;
    private Set<BoxPredictionViewModel> viewModels;

    private IBoundingBoxModelChangeListener listener;
    private IBoundingBoxActionListener actionListener;

    private int selectedBox;
    private int counter;
    private int startX, startY, endX, endY;
    private BoxPredictionViewModel mouseMovedOverBox;
    private BoxPredictionViewModel temporary;

    private Line xAxisDrawingMode;
    private Line yAxisDrawingMode;


    public BoundingBoxPanel(List<BoundingBox> boxes, int selectedIndex) {
        addMouseWheelListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        this.viewModels = new TreeSet<>();
        this.selectedBox = selectedIndex;
    }

    public BoundingBoxPanel(IBoundingBoxModelChangeListener listener, IBoundingBoxActionListener actionListener) {
        addMouseWheelListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        this.viewModels = new TreeSet<>();
        this.selectedBox = -1;
        this.listener = listener;
        this.actionListener = actionListener;
    }


    public void setSelectedBox(int selectedBox) {
        this.selectedBox = selectedBox;
    }

    public void setMovedOver(BoxPredictionViewModel movedOver) {
        if (movedOver != null) {
            mouseMovedOverBox = movedOver;
        }
    }

    public void clearMovedOverArea() {
        if (mouseMovedOverBox != null) {
            //clean color
            getGraphics().fillRect(255, 255, 255, 0);
            mouseMovedOverBox = null;
        }
    }

    private void changeSizeDependingOnZoom() {
        int newWidth = getWidth();
        int newHeight = getHeight();

        if (width * zoomFactor > newWidth) {
            newWidth = (int) (width * zoomFactor);
        }
        if (height * zoomFactor > newHeight) {
            newHeight = (int) (height * zoomFactor);
        }
        if (newWidth != getWidth() || newHeight != getHeight()) {
            setSize(new Dimension(newWidth, newHeight));
        }
    }

    private void drawClassificationBox(Graphics2D g2d, BoxPredictionViewModel model, Point point, int width, int height) {
        double middle = point.getX() + (width / 2.0);

        g2d.setColor(Color.WHITE);

        double classificationBoxX = middle - CLASSIFICATION_BOX_WIDTH / 2;
        double classificationBoxY = point.getY() + height + GAP_BOUNDING_CLASSIFICATION_BOX;

        g2d.setColor(Color.BLACK);

        String prediction = model.getPrediction() == -1 ? "" : String.valueOf(model.getPrediction());

        g2d.drawString(prediction,
                (float) classificationBoxX + TEXT_START_X, (float) classificationBoxY + TEXT_START_Y);
    }

    private void setAxes(MouseEvent e) {
        int x = getRealXLocation(e);
        if (x > width) {
            x = width;
        }
        xAxisDrawingMode = new Line(x, 0, x, height);
        int y = getRealYLocation(e);
        if (y > height) {
            y = height;
        }
        yAxisDrawingMode = new Line(0, y, width, y);
        repaint();
    }

    public Set<BoxPredictionViewModel> getViewModels() {
        return viewModels;
    }

    public void setViewModels(Set<BoxPredictionViewModel> viewModels) {
        this.viewModels = viewModels;
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.BLUE);


        g2d = transformGraphics(g2d);
        changeSizeDependingOnZoom();

        if (drawingEnabled) {
            g2d.drawLine(xAxisDrawingMode.getX1(), xAxisDrawingMode.getY1(),
                    xAxisDrawingMode.getX2(), xAxisDrawingMode.getY2());
            g2d.drawLine(yAxisDrawingMode.getX1(), yAxisDrawingMode.getY1(),
                    yAxisDrawingMode.getX2(), yAxisDrawingMode.getY2());
        }

        if (mouseMovedOverBox != null) {
            g2d.setColor(movedOverColor);
            g2d.fillRect(mouseMovedOverBox.getBoundingBox().getUpLeft().getX(), mouseMovedOverBox.getBoundingBox().getUpLeft().getY(),
                    mouseMovedOverBox.getBoundingBox().getWidth(),
                    mouseMovedOverBox.getBoundingBox().getHeight());
            g2d.setColor(Color.BLUE);
        }

        int i = 0;

        for (BoxPredictionViewModel model : viewModels) {
            BoundingBox box = model.getBoundingBox();

            Point point = box.getUpLeft();
            int width = box.getWidth();
            int height = box.getHeight();

            if (selectedBox == i) {
                g2d.setColor(Color.GREEN);
                g2d.drawRect(point.getX(), point.getY(), width, height);
                g2d.setColor(Color.BLUE);
            } else {
                if (box.getWarningColor() != null) {
                    g2d.setColor(box.getWarningColor());
                }
                g2d.drawRect(point.getX(), point.getY(), width, height);
            }

            drawClassificationBox(g2d, model, point, width, height);
            g2d.setColor(Color.BLUE);

            i++;
        }
    }


    @Override
    public void mouseClicked(MouseEvent e) {

        if (drawingEnabled) {

            if (SwingUtilities.isRightMouseButton(e) && counter != 0) {
                viewModels.remove(temporary);
                clearMovedOverArea();
                counter = 0;
                repaint();
                return;
            }
            if (counter == 0) {
                startX = getRealXLocation(e);
                startY = getRealYLocation(e);
                temporary = new BoxPredictionViewModel(new BoundingBox(Point.create(startX, startY), 1, 1), -1);
                viewModels.add(temporary);
                counter++;
                return;
            }
            endX = getRealXLocation(e);
            endY = getRealYLocation(e);

            viewModels.remove(temporary);
            temporary = null;

            BoundingBox newBox = new BoundingBox(Point.create(startX, startY), endX - startX, endY - startY);
            BoxPredictionViewModel model = new BoxPredictionViewModel(newBox, -1);
            viewModels.add(model);
            counter = 0;

            revalidate();
            repaint();

            listener.modelChanged(model);

        } else {
            if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                BoxPredictionViewModel closestModel = distance(Point.create(getRealXLocation(e), getRealYLocation(e)));
                actionListener.selectedForEdit(closestModel);
            } else if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)) {
                BoxPredictionViewModel closestModel = distance(Point.create(getRealXLocation(e), getRealYLocation(e)));
                actionListener.selected(closestModel);

            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (drawingEnabled) {
            setAxes(e);
        }

        if (drawingEnabled && counter != 0) {
            endX = getRealXLocation(e);
            endY = getRealYLocation(e);
            int newX = endX - startX;
            int newY = endY - startY;

            int maxX = width;
            int maxY = height;
            if (startX + newX > maxX) {
                newX = maxX - startX;
            }
            if (startY + newY > maxY) {
                newY = maxY - startY;
            }

            temporary.getBoundingBox().setWidth(newX);
            temporary.getBoundingBox().setHeight(newY);
            repaint();
        }
        BoxPredictionViewModel closestModel = distance(Point.create(getRealXLocation(e), getRealYLocation(e)));
        actionListener.movedOver(closestModel);

    }


    @Override
    public void statusChanged(boolean enabled) {
        this.drawingEnabled = enabled;
    }


    private BoxPredictionViewModel distance(Point point) {
        int size = viewModels.size();
        double min = Double.MAX_VALUE;
        BoxPredictionViewModel closestViewModel = null;
        double[] distances = new double[size];
        int i = 0;

        for (BoxPredictionViewModel model : viewModels) {
            BoundingBox box = model.getBoundingBox();
            Point upLeft = box.getUpLeft();
            int width = box.getWidth();
            int height = box.getHeight();
            double dx = Math.max(upLeft.getX() - point.getX(), 0);
            dx = Math.max(dx, point.getX() - (upLeft.getX() + width));

            double dy = Math.max(upLeft.getY() - point.getY(), 0);
            dy = Math.max(dy, point.getY() - (upLeft.getY() + height));

            distances[i] = Math.sqrt(dx * dx + dy * dy);
            if (distances[i] < min) {
                min = distances[i];
                closestViewModel = model;
            }
            i++;
        }

        if (min < DISTANCE_TO_RECTANGLE) {
            return closestViewModel;
        }
        return null;
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }
}
