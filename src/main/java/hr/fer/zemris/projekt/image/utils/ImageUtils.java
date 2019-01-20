package hr.fer.zemris.projekt.image.utils;

import hr.fer.zemris.projekt.image.IImageFilter;
import hr.fer.zemris.projekt.image.ImageTransformer;
import hr.fer.zemris.projekt.image.binarization.OtsuBinarization;
import hr.fer.zemris.projekt.image.grayscale.BT709GrayscaleFilter;
import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.image.models.Point;
import hr.fer.zemris.projekt.image.segmentation.ImageSegmentation;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

public class ImageUtils {
    private static List<IImageFilter> imageFilters = new ArrayList<>();

    static {
        imageFilters.add(new BT709GrayscaleFilter());
        imageFilters.add(new OtsuBinarization());
    }

    public static int colorToRGB(int alpha, int red, int green, int blue) {
        int rgb = 0;
        //8-bits for alpha
        rgb += alpha;
        //8-bits for red
        rgb = rgb << 8;
        rgb += red;
        //8-bits for green
        rgb = rgb << 8;
        rgb += green;
        //8-bits for blue
        rgb = rgb << 8;
        rgb += blue;

        return rgb;
    }

    public static int getRedComponent(int rgb) {
        Color color = new Color(rgb);
        return color.getRed();
    }

    public static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }


    public static int[] getHistogramOfGrayImage(BufferedImage grayImage) {
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();

        return getHistogramOfGrayImage(bufferedGrayImageTo2DArray(grayImage), width, height);
//        int[] histogram = new int[256];
//
//
//        for (int x = 0; x < width; x++) {
//            for (int y = 0; y < height; y++) {
//                Color c = new Color(grayImage.getRGB(x, y));
//                histogram[c.getRed()]++;
//            }
//        }
//        return histogram;
    }


    public static int[] getHistogramOfGrayImage(int[][] image, int width, int height) {
        int[] histogram = new int[256];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = new Color(image[x][y]);
                histogram[c.getRed()]++;
            }
        }
        return histogram;
    }

    public static int[][] bufferedGrayImageTo2DArray(BufferedImage grayImage) {
        int width = grayImage.getWidth();
        int height = grayImage.getHeight();

        int[][] imageAs2dArray = new int[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                imageAs2dArray[x][y] = new Color(grayImage.getRGB(x, y)).getRGB();
            }
        }

        return imageAs2dArray;
    }

    public static int[][] image1dArrayTo2dArray(int[] image, int width, int height) {
        int[][] imageAs2d = new int[width][height];

        int x = 0;
        int y = 0;
        for (int i = 0; i < image.length; i++) {
            if (i % (width) == 0 && i != 0) {
                y++;
                x = 0;
            }
            imageAs2d[x][y] = image[i];
            x++;
        }
        return imageAs2d;
    }

    public static int[] image2dArrayTo1dArray(int[][] image, int width, int height) {
        int numberOfPixels = width * height;
        int[] imageAs1dArray = new int[numberOfPixels];
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                imageAs1dArray[index++] = image[x][y];
            }
        }

        return imageAs1dArray;
    }

    public static int[] image2dArrayTo1dGrayArray(int[][] image, int width, int height) {
        int numberOfPixels = width * height;
        int[] imageAs1dArray = new int[numberOfPixels];
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int red = new Color(image[x][y]).getRed();
                imageAs1dArray[index++] = red == 0 ? 0 : 1;
            }
        }

        return imageAs1dArray;
    }

    public static List<BoundingBox> getBoundingBoxesAroundImage(BufferedImage image) {
        ImageTransformer transformer = ImageTransformer.getInstance();
        BufferedImage gray = transformer.transform(image, imageFilters);
        ImageSegmentation segmentation = ImageSegmentation.getInstance();
        List<List<Point>> points = segmentation.getContours(gray);
        return segmentation.getBoundingBoxAroundDigits(points);
    }

    public static List<BufferedImage> getImagesAroundBoundingBoxes(BufferedImage img,
                                                                   List<BoundingBox> boundingBoxes) {
        ImageSegmentation segmentation = ImageSegmentation.getInstance();
        return segmentation.getImagesAroundBoundingBoxes(img, boundingBoxes);
    }
}
