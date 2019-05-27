package hr.fer.zemris.projekt.image.dilation;

import hr.fer.zemris.projekt.image.IImageFilter;
import hr.fer.zemris.projekt.image.managers.ImageManager;

import java.awt.*;
import java.awt.image.BufferedImage;

import static hr.fer.zemris.projekt.gui.providers.ColorsProviders.WHITE;

public class BinaryDilationFilter implements IImageFilter {


    @Override
    public String getFilterName() {
        return "Dilation filter";
    }

    /**
     * I'm using a 3x3 kernel
     * [1, 1, 1
     * 1, 1, 1
     * 1, 1, 1]
     */
    @Override
    public BufferedImage apply(BufferedImage blackWhiteImage) {
        int width = blackWhiteImage.getWidth();
        int height = blackWhiteImage.getHeight();

        BufferedImage dilatedImage = new BufferedImage(width, height, blackWhiteImage.getType());

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = new Color(blackWhiteImage.getRGB(x, y));
                int color = c.getRed();
                if (color == WHITE) {
                    dilate(x, y, dilatedImage, width, height);
                }
                dilatedImage.setRGB(x, y, blackWhiteImage.getRGB(x, y));
            }
        }

        return dilatedImage;
    }

    private void dilate(int x, int y, BufferedImage image, int width, int height) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int currentX = x + dx;
                int currentY = y + dy;
                if (currentX >= 0 && currentX < width && currentY >= 0 && currentY < height) {
                    image.setRGB(currentX, currentY, ImageManager.colorToRGB(255, WHITE, WHITE, WHITE));
                }
            }
        }
    }
}
