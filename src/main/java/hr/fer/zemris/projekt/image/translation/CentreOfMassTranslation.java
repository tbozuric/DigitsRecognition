package hr.fer.zemris.projekt.image.translation;

import hr.fer.zemris.projekt.image.IImageFilter;

import java.awt.*;
import java.awt.image.BufferedImage;

import static hr.fer.zemris.projekt.gui.providers.ColorsProviders.WHITE;

public class CentreOfMassTranslation implements IImageFilter {
    @Override
    public String getFilterName() {
        return "Centre of mass";
    }

    @Override
    public BufferedImage apply(BufferedImage originalImage) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        BufferedImage centeredImage = new BufferedImage(width, height, originalImage.getType());

        int centerX = 0;
        int centerY = 0;
        int num = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = new Color(originalImage.getRGB(x, y));
                if (c.getRed() == WHITE) {
                    centerX += x;
                    centerY += y;
                    num++;
                }
            }
        }
        centerX /= num;
        centerY /= num;

        int centerOfImageX = width / 2;
        int centerOfImageY = height / 2;

        int dx = centerOfImageX - centerX;
        int dy = centerOfImageY - centerY;

        Graphics2D g2d = (Graphics2D) centeredImage.getGraphics();
        g2d.setColor(new Color(0, 0, 0, originalImage.getType()));
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int newPointX = x + dx;
                int newPointY = y + dy;
                if (newPointX >= 0 && newPointY >= 0 && newPointX < width && newPointY < height) {
                    centeredImage.setRGB(newPointX, newPointY, originalImage.getRGB(x, y));
                }
            }
        }

        return centeredImage;
    }
}
