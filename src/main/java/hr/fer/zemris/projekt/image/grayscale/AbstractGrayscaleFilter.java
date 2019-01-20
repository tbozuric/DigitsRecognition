package hr.fer.zemris.projekt.image.grayscale;

import hr.fer.zemris.projekt.image.IImageFilter;
import hr.fer.zemris.projekt.image.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;


public abstract class AbstractGrayscaleFilter implements IImageFilter {


    @Override
    public BufferedImage apply(BufferedImage originalImage) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        int type = originalImage.getType();

        BufferedImage grayImage = new BufferedImage(width, height, type);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = new Color(originalImage.getRGB(x, y));

                int red = c.getRed();
                int green = c.getGreen();
                int blue = c.getBlue();

                int gray = getGrayComponent(red, green, blue);
                grayImage.setRGB(x, y, ImageUtils.colorToRGB(255, gray, gray, gray));
            }
        }

        return grayImage;
    }

    protected abstract int getGrayComponent(int red, int green, int blue);
}
