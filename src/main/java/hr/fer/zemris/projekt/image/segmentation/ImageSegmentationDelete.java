package hr.fer.zemris.projekt.image.segmentation;

import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.image.models.Point;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class ImageSegmentationDelete {


    private static ImageSegmentationDelete imageSegmentation;

    private ImageSegmentationDelete() {
    }


    public static ImageSegmentationDelete getInstance() {
        if (imageSegmentation == null) {
            imageSegmentation = new ImageSegmentationDelete();
        }
        return imageSegmentation;
    }



    public BufferedImage drawBoundingBoxes(BufferedImage image, List<BoundingBox> boundingBoxes) {

        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage clone = new BufferedImage(width, height, image.getType());
        Graphics2D g2d = (Graphics2D) clone.getGraphics();
        g2d.setColor(Color.BLUE);


        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                clone.setRGB(x, y, image.getRGB(x, y));
            }
        }


        for (BoundingBox boundingBox : boundingBoxes) {
            Point upLeft = boundingBox.getUpLeft();
            int minX = upLeft.getX();
            int minY = upLeft.getY();

            g2d.drawRect(minX, minY, boundingBox.getWidth(), boundingBox.getHeight());
        }
        g2d.dispose();
        return clone;
    }
}
