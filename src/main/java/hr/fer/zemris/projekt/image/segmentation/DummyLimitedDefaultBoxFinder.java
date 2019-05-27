package hr.fer.zemris.projekt.image.segmentation;

import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.image.models.Point;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static hr.fer.zemris.projekt.gui.providers.ColorsProviders.WHITE;

public class DummyLimitedDefaultBoxFinder extends AbstractDefaultBoxFilter {

    private static final int NUMBER_OF_DIGITS_OF_IDENTIFIER = 10;

    private List<List<Point>> getContours(BufferedImage binaryImageWithDigits) {

        List<List<Point>> digits = new ArrayList<>();

        int width = binaryImageWithDigits.getWidth();
        int height = binaryImageWithDigits.getHeight();

        boolean foundBlackPixelInColumn;
        boolean foundFirstBlackPixel = false;
        List<Point> points = new ArrayList<>();

        for (int x = 0; x < width; x++) {
            foundBlackPixelInColumn = false;
            for (int y = 0; y < height; y++) {
                Color color = new Color(binaryImageWithDigits.getRGB(x, y));
                if (color.getRed() == WHITE) {
                    if (!foundFirstBlackPixel) {
                        foundFirstBlackPixel = true;
                    }
                    foundBlackPixelInColumn = true;
                    points.add(Point.create(x, y));
                }
            }
            if (!foundBlackPixelInColumn && foundFirstBlackPixel) {
                digits.add(points);
                points = new ArrayList<>();
                foundFirstBlackPixel = false;
            }
        }
        return digits;
    }

    private List<BoundingBox> getBoundingBoxAroundDigits(List<List<Point>> contours) {
        List<BoundingBox> boundingBoxPoints = new ArrayList<>();
        double averageHeight = 0.0;

        for (List<Point> contour : contours) {
            int minX = contour.stream().min(Comparator.comparingInt(Point::getX)).get().getX();
            int minY = contour.stream().min(Comparator.comparingInt(Point::getY)).get().getY();
            int maxX = contour.stream().max(Comparator.comparingInt(Point::getX)).get().getX();
            int maxY = contour.stream().max(Comparator.comparingInt(Point::getY)).get().getY();

            averageHeight += Math.abs(maxY - minY);
            boundingBoxPoints.add(new BoundingBox(Point.create(minX, minY),
                    maxX - minX, maxY - minY));

        }

        double finalAverageHeight = averageHeight / boundingBoxPoints.size();
        boundingBoxPoints = boundingBoxPoints.stream().filter(x -> x.getHeight() > finalAverageHeight / 2.0)
                .collect(Collectors.toList());
        if (boundingBoxPoints.size() == NUMBER_OF_DIGITS_OF_IDENTIFIER) {
            return boundingBoxPoints;
        }

        double averageWidth = 0.0;

        for (BoundingBox box : boundingBoxPoints) {
            averageWidth += box.getWidth();
        }

        averageWidth /= boundingBoxPoints.size();

        int widthOfBox;
        List<BoundingBox> transformedBoxes = new ArrayList<>();
        for (BoundingBox box : boundingBoxPoints) {
            widthOfBox = box.getWidth();

            double difference = (widthOfBox / averageWidth);
            if (widthOfBox < difference) {
                transformedBoxes.add(box);
                continue;
            }

            Point upLeft = box.getUpLeft();
            int minX = upLeft.getX();
            int minY = upLeft.getY();

            //not real round(+0.5)
            int numberOfBoxes = (int) (difference + 0.25);
            if (numberOfBoxes < 1) {
                numberOfBoxes = 1;
            }
            double newWidthOfDigit = widthOfBox / numberOfBoxes;

            for (int i = 0; i < numberOfBoxes; i++) {
                transformedBoxes.add(new BoundingBox(Point.create(minX + i * (int) newWidthOfDigit, minY),
                        (int) newWidthOfDigit, box.getHeight()));
            }
        }
        return transformedBoxes;
    }


    @Override
    public List<BoundingBox> find(BufferedImage image) {
        return getBoundingBoxAroundDigits(getContours(image));
    }
}
