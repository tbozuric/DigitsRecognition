package hr.fer.zemris.projekt.image.segmentation;

import hr.fer.zemris.projekt.filters.Filter;
import hr.fer.zemris.projekt.image.models.BoundingBox;

import java.awt.image.BufferedImage;
import java.util.List;

public interface BoundingBoxFinder {

    List<BoundingBox> find(BufferedImage image);
    List<BoundingBox> filter(List<BoundingBox> boxes , List<Filter<BoundingBox>> filters);
}
