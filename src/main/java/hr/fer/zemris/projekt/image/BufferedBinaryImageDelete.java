package hr.fer.zemris.projekt.image;

import hr.fer.zemris.projekt.image.binarization.OtsuBinarization;
import hr.fer.zemris.projekt.image.grayscale.BT709GrayscaleFilter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class BufferedBinaryImageDelete extends BufferedImage {

    private static final int BINARY_IMAGE = BufferedImage.TYPE_BYTE_BINARY;


    public BufferedBinaryImageDelete(BufferedImage image) {
        super(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        setData(image.getData());
    }

    public BufferedBinaryImageDelete(int width, int height) {
        super(width, height, BINARY_IMAGE);
    }

    public static BufferedBinaryImageDelete transformImageToBinary(BufferedImage input) {
        ImageTransformer transformer = ImageTransformer.getInstance();

        List<IImageFilter> imageFilters = new ArrayList<>();
        imageFilters.add(new BT709GrayscaleFilter());
        imageFilters.add(new OtsuBinarization());

        return new BufferedBinaryImageDelete(transformer.transform(input, imageFilters));
    }


    @Override
    public void setRGB(int x, int y, int rgb) {
        if (rgb != Color.WHITE.getRGB() && rgb != Color.BLACK.getRGB()) {
            throw new UnsupportedOperationException("Image must have only black and white pixels!");
        }
        this.setRGB(x, y, rgb);
    }
}
