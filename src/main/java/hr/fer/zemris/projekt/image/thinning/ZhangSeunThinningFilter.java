package hr.fer.zemris.projekt.image.thinning;

import hr.fer.zemris.projekt.image.IImageFilter;

import java.awt.*;
import java.awt.image.BufferedImage;

import static hr.fer.zemris.projekt.image.managers.ImageManager.colorToRGB;

public class ZhangSeunThinningFilter implements IImageFilter {

    @Override
    public String getFilterName() {
        return "Zhang Seun thinning algorithm";
    }

    @Override
    public BufferedImage apply(BufferedImage blackWhiteImage) {
        int width = blackWhiteImage.getWidth();
        int height = blackWhiteImage.getHeight();

        int[][] image = new int[width][height];
        BufferedImage newImage = new BufferedImage(width, height, blackWhiteImage.getType());

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = new Color(blackWhiteImage.getRGB(x, y));
                image[x][y] = (c.getRed() / 255);
            }
        }


        while (true) {
            int[][] start = new int[width][height];
            for (int x = 0; x < width; x++) {
                System.arraycopy(image[x], 0, start[x], 0, height);
            }

            image = thinningIteration(0, image, width, height);
            image = thinningIteration(1, image, width, height);

            if (areImagesEqual(start, image, width, height)) {
                break;
            }
        }


        int newPixel;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = image[x][y] * 255;
                newPixel = colorToRGB(255, color, color, color);
                newImage.setRGB(x, y, newPixel);
            }
        }
        return newImage;
    }

    private int[][] thinningIteration(int iterationNumber, int[][] image, int width, int height) {
        int[][] marker = new int[width][height];

        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                int p2 = image[x - 1][y];
                int p3 = image[x - 1][y + 1];
                int p4 = image[x][y + 1];
                int p5 = image[x + 1][y + 1];
                int p6 = image[x + 1][y];
                int p7 = image[x + 1][y - 1];
                int p8 = image[x][y - 1];
                int p9 = image[x - 1][y - 1];

                //int A  = (p2 == 0 && p3 == 1) + (p3 == 0 && p4 == 1) +
                //(p4 == 0 && p5 == 1) + (p5 == 0 && p6 == 1) +
                //(p6 == 0 && p7 == 1) + (p7 == 0 && p8 == 1) +
                //(p8 == 0 && p9 == 1) + (p9 == 0 && p2 == 1);
                int c1 = p2 == 0 && p3 == 1 ? 1 : 0; //p2 == 0 && p3 == 1
                int c2 = p3 == 0 && p4 == 1 ? 1 : 0; //p3 == 0 && p4 == 1
                int c3 = p4 == 0 && p5 == 1 ? 1 : 0; //p4 == 0 && p5 == 1
                int c4 = p5 == 0 && p6 == 1 ? 1 : 0; //p5 == 0 && p6 == 1
                int c5 = p6 == 0 && p7 == 1 ? 1 : 0; //p6 == 0 && p7 == 1
                int c6 = p7 == 0 && p8 == 1 ? 1 : 0; //p7 == 0 && p8 == 1
                int c7 = p8 == 0 && p9 == 1 ? 1 : 0; //p8 == 0 && p9 == 1
                int c8 = p9 == 0 && p2 == 1 ? 1 : 0; //p9 == 0 && p2 == 1


                int A = c1 + c2 + c3 + c4 + c5 + c6 + c7 + c8;
                int B = p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9;

                int m1 = iterationNumber == 0 ? (p2 * p4 * p6) : (p2 * p4 * p8);
                int m2 = iterationNumber == 0 ? (p4 * p6 * p8) : (p2 * p6 * p8);

                if (A == 1 && (B >= 2 && B <= 6) && m1 == 0 && m2 == 0) {
                    marker[x][y] = 1;
                }

            }
        }

        int[][] outputImage = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int tmp = 1 - marker[x][y];
                if (image[x][y] == tmp && image[x][y] == 1) {
                    outputImage[x][y] = 1;
                } else {
                    outputImage[x][y] = 0;
                }
            }
        }

        return outputImage;
    }

    private boolean areImagesEqual(int[][] start, int[][] image, int width, int height) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (start[x][y] != image[x][y]) {
                    return false;
                }
            }
        }
        return true;
    }
}
