package hr.fer.zemris.projekt.image.postprocessor;

import hr.fer.zemris.projekt.filters.Filter;
import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.image.segmentation.Filters;
import hr.fer.zemris.projekt.neural.INetwork;
import org.apache.commons.math3.util.Pair;

import java.awt.image.BufferedImage;
import java.util.List;

public class AverageWidthPostProcessor implements IPostProcessor {

    private static AverageWidthPostProcessor postProcessor;

    private double averageWidth;

    private AverageWidthPostProcessor() {
    }

    public static AverageWidthPostProcessor getInstance() {
        if (postProcessor == null) {
            postProcessor = new AverageWidthPostProcessor();
        }
        return postProcessor;
    }

    public void setAverageWidth(double averageWidth) {
        this.averageWidth = averageWidth;
    }

    @Override
    public List<Pair<BoundingBox, Integer>> process(INetwork net, BufferedImage image, BoundingBox box) {
        Filter<BoundingBox> filter = Filters.createAverageWidthFilter(averageWidth);

        if (!filter.getFilter().test(box)) {
            return SlidingWindowAnalyzer.analyze(net, image, box);
        }
        return null;
    }
}
