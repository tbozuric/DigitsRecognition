package hr.fer.zemris.projekt.gui.panels;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;

public class ZoomablePanel extends JPanel implements MouseWheelListener {

    private static final double ZOOM_SCALER = 1.1;

    static double zoomFactor = 1;
    protected static int width;
    protected static int height;

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        //Zoom in
        if (e.isControlDown()) {
            if (e.getWheelRotation() < 0) {
                zoomIn();
            }
            //Zoom out
            if (e.getWheelRotation() > 0) {
                zoomOut();
            }
        } else {
            getParent().dispatchEvent(e);
        }
    }

    public void zoomOut() {
        setZoomFactor(zoomFactor / ZOOM_SCALER);
        revalidate();
        repaint();
    }

    public void zoomIn() {
        setZoomFactor(ZOOM_SCALER * zoomFactor);
        revalidate();
        repaint();
    }


    private void setZoomFactor(double factor) {
        zoomFactor = factor;
        if (zoomFactor < 0.1) {
            zoomFactor = 0.1;
        }
    }


    int getRealXLocation(MouseEvent e) {
        return (int) (e.getX() * (1.0 / zoomFactor));
    }


    int getRealYLocation(MouseEvent e) {
        return (int) (e.getY() * (1.0 / zoomFactor));
    }


    Graphics2D transformGraphics(Graphics2D graphics2D) {
        AffineTransform at = new AffineTransform();
        at.scale(zoomFactor, zoomFactor);
        graphics2D.transform(at);
        return graphics2D;
    }
}
