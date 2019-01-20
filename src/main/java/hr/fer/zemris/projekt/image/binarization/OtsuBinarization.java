package hr.fer.zemris.projekt.image.binarization;

import hr.fer.zemris.projekt.image.IImageFilter;
import hr.fer.zemris.projekt.image.utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

import static hr.fer.zemris.projekt.gui.providers.ColorsProviders.BLACK;
import static hr.fer.zemris.projekt.gui.providers.ColorsProviders.WHITE;

public class OtsuBinarization implements IImageFilter {


    @Override
    public String getFilterName() {
        return "Otsu binarization";
    }

    @Override
    public BufferedImage apply(BufferedImage grayImage) {
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();

        int binarizedPixel;
        int otsuThreshold = calculateOtsuThreshold(grayImage);

        BufferedImage binarizedImage = new BufferedImage(grayImage.getWidth(), grayImage.getHeight(), grayImage.getType());

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = new Color(grayImage.getRGB(x, y));
                int gray = c.getRed();

                if (gray > otsuThreshold) {
                    binarizedPixel = BLACK;
                } else {
                    binarizedPixel = WHITE;
                }

                binarizedPixel = ImageUtils.colorToRGB(255, binarizedPixel, binarizedPixel, binarizedPixel);
                binarizedImage.setRGB(x, y, binarizedPixel);
            }
        }
        return binarizedImage;
    }

    private int calculateOtsuThreshold(BufferedImage grayImage) {
        return calculateOtsuThreshold(ImageUtils.bufferedGrayImageTo2DArray(grayImage),
                grayImage.getWidth(), grayImage.getHeight());
    }


    private int calculateOtsuThreshold(int[][] image, int width, int height) {

        int[] histogram = ImageUtils.getHistogramOfGrayImage(image, width, height);
        int totalNumberOfPixels = width * height;

        float sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += i * histogram[i];
        }

        float sumB = 0;
        int wBackground = 0;
        int wForeground;
        float varianceMaximum = 0;
        int threshold = 0;

        for (int i = 0; i < 256; i++) {
            wBackground += histogram[i];

            if (wBackground == 0) {
                continue;
            }

            wForeground = totalNumberOfPixels - wBackground;

            if (wForeground == 0) {
                break;
            }

            sumB += (float) (i * histogram[i]);
            float miBackground = sumB / wBackground;
            float miForeground = (sum - sumB) / wForeground;

            float varBetween = (float) wBackground * (float) wForeground * (float) Math.pow((miBackground - miForeground), 2.0);

            if (varBetween > varianceMaximum) {
                varianceMaximum = varBetween;
                threshold = i;
            }
        }
        return threshold;

    }
}
