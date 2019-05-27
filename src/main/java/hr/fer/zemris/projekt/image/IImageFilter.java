package hr.fer.zemris.projekt.image;

import java.awt.image.BufferedImage;

public interface IImageFilter {

    String getFilterName();

    BufferedImage apply(BufferedImage originalImage);

}
