package hr.fer.zemris.projekt.image.interpolation;

import hr.fer.zemris.projekt.image.IImageFilter;

import java.awt.image.BufferedImage;

public class NearestNeighborInterpolation implements IImageFilter {

    private int newWidth;
    private int newHeight;

    public NearestNeighborInterpolation(int newWidth, int newHeight) {
        this.newWidth = newWidth;
        this.newHeight = newHeight;
    }

    @Override
    public String getFilterName() {
        return "Nearest Neighbor interpolation";
    }

    @Override
    public BufferedImage apply(BufferedImage originalImage) {
        if (newWidth <= 0 || newHeight <= 0) {
            throw new IllegalArgumentException("Width and height must be non-negative integer numbers.");
        }

        int currentWidth = originalImage.getWidth();
        int currentHeight = originalImage.getHeight();

        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, originalImage.getType());

        int xRatio = ((currentWidth << 16) / newWidth) + 1;
        int yRatio = ((currentHeight << 16) / newHeight) + 1;

        int xBefore;
        int yBefore;

        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                xBefore = (x * xRatio) >> 16;
                yBefore = (y * yRatio) >> 16;
                scaledImage.setRGB(x, y, originalImage.getRGB(xBefore, yBefore));
            }
        }
        return scaledImage;
    }

}
