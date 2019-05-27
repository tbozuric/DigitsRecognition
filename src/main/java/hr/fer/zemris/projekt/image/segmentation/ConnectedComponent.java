package hr.fer.zemris.projekt.image.segmentation;

import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.image.models.Point;
import hr.fer.zemris.projekt.image.structures.UnionFind;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static hr.fer.zemris.projekt.gui.providers.ColorsProviders.WHITE;

public class ConnectedComponent extends AbstractDefaultBoxFilter {

    private static final int dxTop = 0;
    private static final int dyTop = -1;

    private static final int dxLeft = -1;
    private static final int dyLeft = 0;


    public ConnectedComponent() {
    }

    public Map<Point, Integer> getConnectedComponents(BufferedImage binaryImage) {
        Map<Point, Integer> labels = new TreeMap<>();

        int width = binaryImage.getWidth();
        int height = binaryImage.getHeight();

        UnionFind unionFind = new UnionFind(width * height);
        int currentLabel = 1;

        //first pass of the algorithm
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color color = new Color(binaryImage.getRGB(x, y));
                if (color.getRed() == WHITE) {

                    int leftLabel = labels.getOrDefault(Point.create(x + dxLeft, y + dyLeft), -1);
                    int topLabel = labels.getOrDefault(Point.create(x + dxTop, y + dyTop), -1);


                    boolean hasLeftPixel = leftLabel != -1;
                    boolean hasTopPixel = topLabel != -1;

                    Point point = Point.create(x, y);

                    if (!hasLeftPixel && !hasTopPixel) {
                        labels.put(point, currentLabel++);
                    } else if (hasLeftPixel && hasTopPixel) {

                        if (leftLabel != topLabel) {
                            if (leftLabel < topLabel) {
                                labels.put(point, leftLabel);
                                unionFind.unify(leftLabel, topLabel);
                            } else {
                                labels.put(point, topLabel);
                                unionFind.unify(topLabel, leftLabel);
                            }
                        } else {
                            labels.put(point, leftLabel);
                        }
                    } else if (hasLeftPixel) {
                        labels.put(point, leftLabel);

                    } else {
                        labels.put(point, topLabel);
                    }
                }
            }
        }
        //second pass of the algorithm
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Point point = Point.create(x, y);
                Color color = new Color(binaryImage.getRGB(x, y));
                if (color.getRed() == WHITE) {
                    int label = labels.get(point);
                    if (unionFind.componentSize(label) != 1) {
                        int labelParent = unionFind.find(label);
                        labels.put(point, labelParent);
                    }
                }
            }
        }
        return labels;
    }

    @Override
    public List<BoundingBox> find(BufferedImage image) {
        Map<Point, Integer> labels = getConnectedComponents(image);
        List<BoundingBox> boxes = new ArrayList<>();

        Map<Integer, List<Point>> valueMap = labels.keySet().stream().collect(Collectors.groupingBy(labels::get));

        for (Map.Entry<Integer, List<Point>> entry : valueMap.entrySet()) {
            Point leftCorner = Point.create(image.getWidth(), image.getHeight());
            Point rightCorner = Point.create(0, 0);
            for (Point point : entry.getValue()) {
                int x = point.getX();
                int y = point.getY();

                if (x < leftCorner.getX()) {
                    leftCorner.setX(x);
                }

                if (x > rightCorner.getX()) {
                    rightCorner.setX(x);
                }

                if (y < leftCorner.getY()) {
                    leftCorner.setY(y);
                }

                if (y > rightCorner.getY()) {
                    rightCorner.setY(y);
                }
            }
            int xUpLeft = leftCorner.getX();
            int yUpLeft = leftCorner.getY();
            int height = rightCorner.getY() - yUpLeft;
            int width = rightCorner.getX() - xUpLeft;
            boxes.add(new BoundingBox(Point.create(xUpLeft, yUpLeft), width, height));
        }
        return boxes;
    }
}
