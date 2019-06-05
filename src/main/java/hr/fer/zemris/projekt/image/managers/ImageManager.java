package hr.fer.zemris.projekt.image.managers;

import hr.fer.zemris.projekt.filters.FilterAggregator;
import hr.fer.zemris.projekt.image.ImageBuilder;
import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.image.models.Point;
import hr.fer.zemris.projekt.image.postprocessor.AverageWidthPostProcessor;
import hr.fer.zemris.projekt.image.postprocessor.EntropyConfidenceBasedPostProcessor;
import hr.fer.zemris.projekt.image.postprocessor.IPostProcessor;
import hr.fer.zemris.projekt.image.segmentation.BoundingBoxFinder;
import hr.fer.zemris.projekt.image.segmentation.ConnectedComponent;
import hr.fer.zemris.projekt.image.segmentation.Filters;
import hr.fer.zemris.projekt.neural.INetwork;
import org.apache.commons.math3.util.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ImageManager {

    private static ImageBuilder grayImage = new ImageBuilder();

    static {
        grayImage.grayscale();
        grayImage.binarization();
        grayImage.dilation();
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

        ImageBuilder imageBuilder = new ImageBuilder();

        return imageBuilder.grayscale()
                .binarization()
                .border()
                .dilation()
                .interpolation(desiredWidth, desiredHeight)
                .centreOfMassTranslation().build(image);

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
        BufferedImage gray = grayImage.build(image);

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

    public static void saveDigitsFromImages(Path inputDirectory, Path outputDirectory) throws IOException {
        File dir = new File(inputDirectory.toString());
        File[] directoryListing = dir.listFiles();

        if (directoryListing != null) {
            for (File child : directoryListing) {
                BufferedImage img = ImageIO.read(child);
                StringBuilder nameOfImage = new StringBuilder(child.getName());
                nameOfImage = new StringBuilder(nameOfImage.substring(0, nameOfImage.indexOf(".")));

                List<BufferedImage> digitsFromImage = getImagesAroundBoundingBoxes(img,
                        getBoundingBoxesAroundImage(img, new ConnectedComponent()));
                int counter = 0;
                String finalNameOfImage;
                for (BufferedImage bufferedImage : digitsFromImage) {
                    finalNameOfImage = nameOfImage + "_" + counter++ + ".png";
                    Path newImage = Paths.get(outputDirectory.toString(), finalNameOfImage);
                    ImageIO.write(bufferedImage, "png", new File(newImage.toString()));
                }
            }
        }
    }

    public static void tranformImagesToBinary(Path inputDirectory, Path outputDirectory) throws IOException {
        File dir = new File(inputDirectory.toString());
        File[] directoryListing = dir.listFiles();

        ImageBuilder imageBuilder = new ImageBuilder();
        imageBuilder.grayscale()
                .binarization()
                .border()
                .dilation()
                .interpolation()
                .centreOfMassTranslation();


        if (directoryListing != null) {
            for (File child : directoryListing) {
                BufferedImage img = ImageIO.read(child);
                String nameOfImage = child.getName();

                BufferedImage gray = imageBuilder.build(img);

                Path path = Paths.get(outputDirectory.toString(), nameOfImage);
                ImageIO.write(gray, "png", path.toFile());
            }
        }
    }
}
