package hr.fer.zemris.projekt.image;

import hr.fer.zemris.projekt.image.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ImageTransformer {

    private List<IImageFilter> imageFilters;
    private static ImageTransformer transformer;


    private ImageTransformer() {
        imageFilters = new ArrayList<>();
    }


    public static ImageTransformer getInstance() {
        if (transformer == null) {
            transformer = new ImageTransformer();
        }
        return transformer;
    }

    public void addImageFilter(IImageFilter filter) {
        imageFilters.add(filter);
    }

    public void removeImageFilter(IImageFilter filter) {
        if (imageFilters.contains(filter)) {
            imageFilters.remove(filter);
        }
    }


    public BufferedImage transform(BufferedImage image) {
        BufferedImage clone = ImageUtils.deepCopy(image);
        for (IImageFilter filter : imageFilters) {
            clone = filter.apply(clone);
        }
        return clone;
    }

    public BufferedImage transform(BufferedImage image, List<IImageFilter> filters) {
        BufferedImage clone = ImageUtils.deepCopy(image);
        for (IImageFilter filter : filters) {
            clone = filter.apply(clone);
        }
        return clone;
    }


    public BufferedImage transform(BufferedImage image, IImageFilter filter) {
        BufferedImage clone = ImageUtils.deepCopy(image);
        return filter.apply(clone);
    }


    public List<IImageFilter> getImageFilters() {
        return imageFilters;
    }

    public void setImageFilters(List<IImageFilter> imageFilters) {
        this.imageFilters = imageFilters;
    }

}
