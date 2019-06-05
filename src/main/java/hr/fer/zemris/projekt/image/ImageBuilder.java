package hr.fer.zemris.projekt.image;

import hr.fer.zemris.projekt.image.binarization.OtsuBinarization;
import hr.fer.zemris.projekt.image.border.BorderImage;
import hr.fer.zemris.projekt.image.dilation.BinaryDilationFilter;
import hr.fer.zemris.projekt.image.grayscale.BT709GrayscaleFilter;
import hr.fer.zemris.projekt.image.interpolation.NearestNeighborInterpolation;
import hr.fer.zemris.projekt.image.translation.CentreOfMassTranslation;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ImageBuilder {

    private static final int DEFAULT_IMAGE_WIDTH = 28;
    private static final int DEFAULT_IMAGE_HEIGHT = 28;

    private static final IImageFilter GRAYSCALE = new BT709GrayscaleFilter();
    private static final IImageFilter BINARIZATION = new OtsuBinarization();
    private static final IImageFilter BORDER = new BorderImage();
    private static final IImageFilter DILATION = new BinaryDilationFilter();
    private static final IImageFilter INTERPOLATION = new NearestNeighborInterpolation(DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT);
    private static final IImageFilter CENTRE_OF_MASS_TRANSLATION = new CentreOfMassTranslation();

    private List<IImageFilter> filters;

    public ImageBuilder() {
        filters = new ArrayList<>();
    }

    public ImageBuilder grayscale() {
        filters.add(GRAYSCALE);
        return this;
    }

    public ImageBuilder binarization() {
        filters.add(BINARIZATION);
        return this;
    }


    public ImageBuilder border() {
        filters.add(BORDER);
        return this;
    }

    public ImageBuilder dilation() {
        filters.add(DILATION);
        return this;
    }

    public ImageBuilder interpolation() {
        filters.add(INTERPOLATION);
        return this;
    }

    public ImageBuilder interpolation(int width, int height) {
        filters.add(new NearestNeighborInterpolation(width, height));
        return this;
    }

    public ImageBuilder centreOfMassTranslation() {
        filters.add(CENTRE_OF_MASS_TRANSLATION);
        return this;
    }

    public BufferedImage build(BufferedImage image) {
        ImageTransformer transformer = ImageTransformer.getInstance();
        return transformer.transform(image, filters);
    }


}
