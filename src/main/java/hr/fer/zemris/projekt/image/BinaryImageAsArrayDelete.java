package hr.fer.zemris.projekt.image;

import java.awt.*;
import java.awt.image.BufferedImage;

public class BinaryImageAsArrayDelete extends BufferedImage {

    private int[][] image;

    public BinaryImageAsArrayDelete(int[][] image) {
        super(image.length, image[0].length, BufferedImage.TYPE_4BYTE_ABGR);
        this.image = image;
    }

    public BinaryImageAsArrayDelete(BufferedImage image) {
        super(image.getWidth(), image.getHeight(), image.getType());
        int width = image.getWidth();
        int height = image.getHeight();

        this.image = new int[height][width];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y);
                if (rgb != Color.WHITE.getRGB() && rgb != Color.BLACK.getRGB()) {
                    throw new UnsupportedOperationException("Image must have only white and black pixels!");
                }
                this.image[x][y] = rgb;
            }
        }

    }

    public BinaryImageAsArrayDelete(int height, int width) {
        super(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        image = new int[height][width];
    }

    @Override
    public int getWidth() {
        return image.length;
    }

    @Override
    public int getHeight() {
        return image[0].length;
    }

    @Override
    public int getRGB(int x, int y) {
        return image[x][y];
    }

    @Override
    public synchronized void setRGB(int x, int y, int rgb) {
        if (rgb != Color.WHITE.getRGB() && rgb != Color.BLACK.getRGB()) {
            throw new UnsupportedOperationException("Image must have only white and black pixels!");
        }
        image[x][y] = rgb;
    }
}
