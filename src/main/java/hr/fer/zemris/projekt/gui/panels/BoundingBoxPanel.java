package hr.fer.zemris.projekt.gui.panels;

import hr.fer.zemris.projekt.gui.listeners.IBoundingBoxModelChangeListener;
import hr.fer.zemris.projekt.gui.listeners.IBoundingBoxSelectListener;
import hr.fer.zemris.projekt.gui.listeners.IDrawingStatusListener;
import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.image.models.Point;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

public class BoundingBoxPanel extends ZoomablePanel implements MouseMotionListener, MouseListener, IDrawingStatusListener {

    private static final double DISTANCE_TO_RECTANGLE = 0.5;
    private static final int CLASSIFICATION_BOX_WIDTH = 16;
    private static final int CLASSIFICATION_BOX_HEIGHT = 20;
    private static final int GAP_BOUNDING_CLASSIFICATION_BOX = 5;
    private static final int TEXT_START_X = 3;
    private static final int TEXT_START_Y = 15;

    private boolean drawingEnabled = false;
    private List<BoundingBox> boxes;
    private List<Integer> classifications;

    private IBoundingBoxModelChangeListener listener;
    private IBoundingBoxSelectListener selectListener;

    private int selectedBox;
    private int counter;
    private int startX, startY, endX, endY;
    private BoundingBox temporary;


    public BoundingBoxPanel(List<BoundingBox> boxes, int selectedIndex) {
        addMouseWheelListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        this.boxes = boxes;
        this.selectedBox = selectedIndex;
    }

    public BoundingBoxPanel(IBoundingBoxModelChangeListener listener, IBoundingBoxSelectListener selectListener) {
        addMouseWheelListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        this.boxes = new ArrayList<>();
        this.classifications = new ArrayList<>();
        this.selectedBox = -1;
        this.listener = listener;
        this.selectListener = selectListener;
    }

    public void setBoxes(List<BoundingBox> boxes) {
        this.boxes = boxes;
    }

    public List<BoundingBox> getBoxes() {
        return boxes;
    }

    public void setSelectedBox(int selectedBox) {
        this.selectedBox = selectedBox;
    }

    public List<Integer> getClassifications() {
        return classifications;
    }

    public void setClassifications(List<Integer> classifications) {
        this.classifications = classifications;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.BLUE);

        g2d = transformGraphics(g2d);
        changeSizeDependingOnZoom();

        int size = boxes.size();
        for (int i = 0; i < size; i++) {
            BoundingBox box = boxes.get(i);
            Point point = box.getUpLeft();
            int width = box.getWidth();
            int height = box.getHeight();

            if (selectedBox == i) {
                g2d.setColor(Color.RED);
                g2d.drawRect(point.getX(), point.getY(), width, height);
                g2d.setColor(Color.BLUE);
            } else {
                g2d.drawRect(point.getX(), point.getY(), width, height);
            }

            drawClassificationBox(g2d, i, point, width, height);
            g2d.setColor(Color.BLUE);
        }
    }

    private void changeSizeDependingOnZoom() {
        int newWidth = getWidth();
        int newHeight = getHeight();

        if (width > newWidth) {
            newWidth = width;
        }
        if (height > newHeight) {
            newHeight = height;
        }
        if (newWidth != getWidth() || newHeight != getHeight()) {
            setSize(new Dimension(newWidth, newHeight));
        }
    }

    private void drawClassificationBox(Graphics2D g2d, int i, Point point, int width, int height) {
        double middle = point.getX() + (width / 2.0);

        g2d.setColor(Color.WHITE);

        double classificationBoxX = middle - CLASSIFICATION_BOX_WIDTH / 2;
        double classificationBoxY = point.getY() + height + GAP_BOUNDING_CLASSIFICATION_BOX;

        g2d.fillRect((int) classificationBoxX, (int) classificationBoxY, CLASSIFICATION_BOX_WIDTH, CLASSIFICATION_BOX_HEIGHT);
        g2d.setColor(Color.BLACK);

        if (i < classifications.size()) {
            g2d.drawString(String.valueOf(classifications.get(i)),
                    (float) classificationBoxX + TEXT_START_X, (float) classificationBoxY + TEXT_START_Y);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {

        if (drawingEnabled) {
            if (SwingUtilities.isRightMouseButton(e) && counter != 0) {
                boxes.remove(boxes.size() - 1);
                counter = 0;
                repaint();
                return;
            }
            if (counter == 0) {
                startX = getRealXLocation(e);
                startY = getRealYLocation(e);
                temporary = new BoundingBox(new Point(startX, startY), 1, 1);
                boxes.add(temporary);
                counter++;
                return;
            }
            endX = getRealXLocation(e);
            endY = getRealYLocation(e);

            temporary = null;
            boxes.remove(boxes.size() - 1);

            BoundingBox newBox = new BoundingBox(new Point(startX, startY), endX - startX, endY - startY);

            boxes.add(newBox);
            counter = 0;

            revalidate();
            repaint();

            listener.modelChanged(newBox);

        } else {
            if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                int index = distance(new Point(getRealXLocation(e), getRealYLocation(e)));
                selectListener.selectedForEdit(index);
            } else if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)) {
                int index = distance(new Point(getRealXLocation(e), getRealYLocation(e)));
                selectListener.selected(index);

            }
        }
    }


    @Override
    public void mouseMoved(MouseEvent e) {
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
            temporary.setWidth(newX);
            temporary.setHeight(newY);
            repaint();
        }
    }

    @Override
    public void statusChanged(boolean enabled) {
        this.drawingEnabled = enabled;
    }


    private int distance(Point point) {
        int size = boxes.size();
        double min = Double.MAX_VALUE;
        int index = -1;
        double[] distances = new double[size];

        for (int i = 0; i < size; i++) {
            Point upLeft = boxes.get(i).getUpLeft();
            int width = boxes.get(i).getWidth();
            int height = boxes.get(i).getHeight();
            double dx = Math.max(upLeft.getX() - point.getX(), 0);
            dx = Math.max(dx, point.getX() - (upLeft.getX() + width));

            double dy = Math.max(upLeft.getY() - point.getY(), 0);
            dy = Math.max(dy, point.getY() - (upLeft.getY() + height));

            distances[i] = Math.sqrt(dx * dx + dy * dy);
            if (distances[i] < min) {
                min = distances[i];
                index = i;
            }
        }

        if (min < DISTANCE_TO_RECTANGLE) {
            return index;
        }
        return -1;
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
