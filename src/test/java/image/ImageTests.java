package image;

import hr.fer.zemris.projekt.image.BinaryImageAsArrayDelete;
import hr.fer.zemris.projekt.image.BufferedBinaryImageDelete;
import hr.fer.zemris.projekt.image.IImageFilter;
import hr.fer.zemris.projekt.image.ImageTransformer;
import hr.fer.zemris.projekt.image.dilation.BinaryDilationFilter;
import hr.fer.zemris.projekt.image.segmentation.ConnectedComponent;
import hr.fer.zemris.projekt.image.models.Point;
import org.junit.Assert;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ImageTests {

    @Test
    public void componentConnectedAnalysisImageAsArray() {
        BinaryImageAsArrayDelete imageAsArray = new BinaryImageAsArrayDelete(8, 8);
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                imageAsArray.setRGB(x, y, Color.WHITE.getRGB());
            }
        }
        imageAsArray.setRGB(0, 2, Color.BLACK.getRGB());
        imageAsArray.setRGB(0, 6, Color.BLACK.getRGB());
        imageAsArray.setRGB(1, 2, Color.BLACK.getRGB());
        imageAsArray.setRGB(1, 4, Color.BLACK.getRGB());
        imageAsArray.setRGB(1, 6, Color.BLACK.getRGB());
        imageAsArray.setRGB(2, 4, Color.BLACK.getRGB());
        imageAsArray.setRGB(2, 5, Color.BLACK.getRGB());
        imageAsArray.setRGB(2, 6, Color.BLACK.getRGB());
        imageAsArray.setRGB(3, 0, Color.BLACK.getRGB());
        imageAsArray.setRGB(3, 1, Color.BLACK.getRGB());
        imageAsArray.setRGB(3, 2, Color.BLACK.getRGB());
        imageAsArray.setRGB(3, 3, Color.BLACK.getRGB());
        imageAsArray.setRGB(3, 4, Color.BLACK.getRGB());
        imageAsArray.setRGB(3, 5, Color.BLACK.getRGB());
        imageAsArray.setRGB(3, 6, Color.BLACK.getRGB());
        imageAsArray.setRGB(4, 4, Color.BLACK.getRGB());
        imageAsArray.setRGB(4, 6, Color.BLACK.getRGB());

        imageAsArray.setRGB(5, 0, Color.BLACK.getRGB());
        imageAsArray.setRGB(5, 1, Color.BLACK.getRGB());
        imageAsArray.setRGB(5, 2, Color.BLACK.getRGB());
        imageAsArray.setRGB(5, 4, Color.BLACK.getRGB());
        imageAsArray.setRGB(5, 6, Color.BLACK.getRGB());


        imageAsArray.setRGB(6, 4, Color.BLACK.getRGB());
        imageAsArray.setRGB(6, 5, Color.BLACK.getRGB());
        imageAsArray.setRGB(6, 6, Color.BLACK.getRGB());

        imageAsArray.setRGB(7, 4, Color.BLACK.getRGB());


        ConnectedComponent connectedComponentLabelling = new ConnectedComponent();
        Map<Point, Integer> result = connectedComponentLabelling.getConnectedComponents(imageAsArray);

        Map<Integer, List<Point>> groupedPixels = result.entrySet().stream().collect(Collectors.groupingBy(
                Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));


        Assert.assertEquals(groupedPixels.size(), 4);

    }


    @Test
    public void connectedComponentAnalysisBufferedImage() {
        try {

            BufferedImage bufferedImage = ImageIO.read(new File("src/main/resources/input/input1.png"));

            BufferedBinaryImageDelete binaryImage = BufferedBinaryImageDelete.transformImageToBinary(bufferedImage);

            IImageFilter dilatation = new BinaryDilationFilter();
            ImageTransformer transformer = ImageTransformer.getInstance();

            BufferedBinaryImageDelete dilatatedBinaryImage = new BufferedBinaryImageDelete(transformer.transform(binaryImage, dilatation));

            ConnectedComponent connectedComponentLabelling = new ConnectedComponent();

            Map<Point, Integer> result = connectedComponentLabelling.getConnectedComponents(dilatatedBinaryImage);
            Map<Integer, List<Point>> groupedPixels = result.entrySet().stream().collect(Collectors.groupingBy(
                    Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

            Assert.assertEquals(9, groupedPixels.size());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

