package hr.fer.zemris.projekt.image.border;

import hr.fer.zemris.projekt.image.IImageFilter;
import hr.fer.zemris.projekt.image.managers.ImageManager;

import java.awt.image.BufferedImage;

import static hr.fer.zemris.projekt.gui.providers.ColorsProviders.BLACK;

public class BorderImage implements IImageFilter {

    private static final int BORDER_WIDTH = 8;

    @Override
    public String getFilterName() {
        return "Add border";
    }

    @Override
    public BufferedImage apply(BufferedImage originalImage) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        int newWidth = width + 2 * BORDER_WIDTH;
        int newHeight = height + 2 * BORDER_WIDTH;

        BufferedImage clone = new BufferedImage(newWidth, newHeight, originalImage.getType());


        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                if (x < BORDER_WIDTH || x > BORDER_WIDTH + width || y < BORDER_WIDTH || y > BORDER_WIDTH + height) {
                    clone.setRGB(x, y, ImageManager.colorToRGB(255, BLACK, BLACK, BLACK));
                }
            }
        }

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                clone.setRGB(x + BORDER_WIDTH, y + BORDER_WIDTH, originalImage.getRGB(x, y));
            }
        }

        return clone;
    }
}
