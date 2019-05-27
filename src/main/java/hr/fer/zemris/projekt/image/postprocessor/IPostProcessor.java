package hr.fer.zemris.projekt.image.postprocessor;

import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.neural.INetwork;
import org.apache.commons.math3.util.Pair;

import java.awt.image.BufferedImage;
import java.util.List;

public interface IPostProcessor {
    List<Pair<BoundingBox, Integer>> process(INetwork net, BufferedImage image, BoundingBox box);
}
