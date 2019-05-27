package hr.fer.zemris.projekt.image.postprocessor;

import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.filters.Filter;
import hr.fer.zemris.projekt.image.segmentation.Filters;
import hr.fer.zemris.projekt.neural.INetwork;
import org.apache.commons.math3.util.Pair;

import java.awt.image.BufferedImage;
import java.util.List;

public class EntropyConfidenceBasedPostProcessor implements IPostProcessor {


    private static EntropyConfidenceBasedPostProcessor postProcessor= new EntropyConfidenceBasedPostProcessor();

    private EntropyConfidenceBasedPostProcessor(){

    }

    public static EntropyConfidenceBasedPostProcessor getInstance(){
        return postProcessor;
    }

    @Override
    public List<Pair<BoundingBox, Integer>> process(INetwork net, BufferedImage image, BoundingBox box) {
        Filter<BufferedImage> entropyBasedFilter = Filters.createEntropyBasedFilter(net);
        Filter<BufferedImage> confidenceFilter = Filters.createConfidenceFilter(net);

        if (!entropyBasedFilter.getFilter().test(image) && !confidenceFilter.getFilter().test(image)) {
            return SlidingWindowAnalyzer.analyze(net, image, box);
        }

        return null;
    }
}
