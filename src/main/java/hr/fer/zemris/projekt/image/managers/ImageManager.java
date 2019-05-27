package hr.fer.zemris.projekt.image.managers;

import hr.fer.zemris.projekt.filters.FilterAggregator;
import hr.fer.zemris.projekt.image.IImageFilter;
import hr.fer.zemris.projekt.image.ImageTransformer;
import hr.fer.zemris.projekt.image.binarization.OtsuBinarization;
import hr.fer.zemris.projekt.image.border.BorderImage;
import hr.fer.zemris.projekt.image.dilation.BinaryDilationFilter;
import hr.fer.zemris.projekt.image.grayscale.BT709GrayscaleFilter;
import hr.fer.zemris.projekt.image.interpolation.NearestNeighborInterpolation;
import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.image.models.Point;
import hr.fer.zemris.projekt.image.postprocessor.AverageWidthPostProcessor;
import hr.fer.zemris.projekt.image.postprocessor.EntropyConfidenceBasedPostProcessor;
import hr.fer.zemris.projekt.image.postprocessor.IPostProcessor;
import hr.fer.zemris.projekt.image.segmentation.BoundingBoxFinder;
import hr.fer.zemris.projekt.image.segmentation.Filters;
import hr.fer.zemris.projekt.image.translation.CentreOfMassTranslation;
import hr.fer.zemris.projekt.neural.INetwork;
import org.apache.commons.math3.util.Pair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

public class ImageManager {
    private static List<IImageFilter> imageFilters = new ArrayList<>();

    static {
        imageFilters.add(new BT709GrayscaleFilter());
        imageFilters.add(new OtsuBinarization());
        imageFilters.add(new BinaryDilationFilter());
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


    public static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.getRaster().createWritableChild(0, 0, bi.getWidth(), bi.getHeight(),
                0, 0, null);

        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }


    public static BufferedImage transformImageToBinary(BufferedImage image, int desiredWidth, int desiredHeight) {
        ImageTransformer transformer = ImageTransformer.getInstance();
        List<IImageFilter> filters = new ArrayList<>();

        filters.add(new BT709GrayscaleFilter());
        filters.add(new OtsuBinarization());
        filters.add(new BorderImage());
        filters.add(new BinaryDilationFilter());
        filters.add(new NearestNeighborInterpolation(desiredWidth, desiredHeight));
        filters.add(new CentreOfMassTranslation());

        return transformer.transform(image, filters);
    }


    public static int[] getHistogramOfGrayImage(BufferedImage image, int width, int height) {
        int[] histogram = new int[256];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color c = new Color(image.getRGB(x, y));
                histogram[c.getRed()]++;
            }
        }
        return histogram;
    }


    public static List<BoundingBox> getBoundingBoxesAroundImage(BufferedImage image, BoundingBoxFinder finder) {
        ImageTransformer transformer = ImageTransformer.getInstance();
        BufferedImage gray = transformer.transform(image, imageFilters);

        List<BoundingBox> boxes = finder.find(gray);
        FilterAggregator<BoundingBox> aggregator = new FilterAggregator<>();
        aggregator.addFilter(Filters.createNoiseFilterByAverageHeight(boxes));

        return finder.filter(finder.find(gray), aggregator.getFilters());
    }

    public static List<Pair<BoundingBox, Integer>> postProcessImage(INetwork net, BufferedImage image, BoundingBox box,
                                                                    double averageWidth) {

        IPostProcessor averageWidthProcessor = AverageWidthPostProcessor.getInstance();
        IPostProcessor entropyProcessor = EntropyConfidenceBasedPostProcessor.getInstance();

        ((AverageWidthPostProcessor) averageWidthProcessor).setAverageWidth(averageWidth);

        List<Pair<BoundingBox, Integer>> boxes = averageWidthProcessor.process(net, image, box);

        if (boxes == null) {
            boxes = entropyProcessor.process(net, image, box);
            return boxes;
        } else {
            return boxes;
        }
    }


    public static List<BufferedImage> getImagesAroundBoundingBoxes(BufferedImage image, List<BoundingBox> boundingBoxes) {

        List<BufferedImage> bufferedImages = new ArrayList<>();

        int imageType = image.getType();

        for (BoundingBox boundingBox : boundingBoxes) {
            int boxWidth = boundingBox.getWidth();
            int boxHeight = boundingBox.getHeight();

            BufferedImage bufferedImage = new BufferedImage(boxWidth, boxHeight, imageType);
            Point upLeft = boundingBox.getUpLeft();
            int x = upLeft.getX();
            int y = upLeft.getY();

            int newX = 0;
            int newY = 0;


            int maxX = x + boxWidth;
            int maxY = y + boxHeight;

            for (int i = x; i < maxX; i++) {
                for (int j = y; j < maxY; j++) {
                    bufferedImage.setRGB(newX, newY++, image.getRGB(i, j));
                }
                newY = 0;
                newX++;
            }

            bufferedImages.add(bufferedImage);
        }
        return bufferedImages;
    }
}
