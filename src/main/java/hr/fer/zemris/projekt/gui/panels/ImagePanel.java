package hr.fer.zemris.projekt.gui.panels;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImagePanel extends ZoomablePanel {

    private BufferedImage image;

    public ImagePanel(BufferedImage image) {
        addMouseWheelListener(this);
        this.image = image;
    }

    public ImagePanel() {
        addMouseWheelListener(this);
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d = transformGraphics(g2d);
        g2d.drawImage(image, 0, 0, null);
        changeSizeDependingOnZoom();
        g2d.dispose();
    }

    private void changeSizeDependingOnZoom() {
        if (image != null) {
            width = (int) (image.getWidth() * zoomFactor);
            height = (int) (image.getHeight() * zoomFactor);
            setSize(new Dimension(width, height));
        }
    }
}
