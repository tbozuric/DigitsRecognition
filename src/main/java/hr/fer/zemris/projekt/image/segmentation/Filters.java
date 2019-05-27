package hr.fer.zemris.projekt.image.segmentation;

import hr.fer.zemris.projekt.filters.Filter;
import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.math.Entropy;
import hr.fer.zemris.projekt.neural.INetwork;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public class Filters {

    private static final float MAXIMUM_PERPLEXITY = 3.2f;
    private static final float MINIMUM_CONFIDENCE = 0.50f;
    private static final float AVERAGE_WIDTH_SCALING_FACTOR = 1.5f;



    public static Filter<BoundingBox> createNoiseFilterByAverageHeight(List<BoundingBox> boxes) {
        int totalHeight = 0;

        for (BoundingBox box : boxes) {
            totalHeight += box.getHeight();
        }

        float averageHeight = totalHeight / (float) boxes.size();

        return new Filter<>(boundingBox -> boundingBox.getHeight() > averageHeight / 2.0);
    }


    public static Filter<BufferedImage> createEntropyBasedFilter(INetwork net) {
        Entropy entropy = Entropy.getInstance();
        return new Filter<>(image -> entropy.perplexity(net.predictOutputProbabilities(image)) < MAXIMUM_PERPLEXITY);
    }

    public static Filter<BufferedImage> createConfidenceFilter(INetwork net) {
        return new Filter<>(image -> {
            boolean isPresent = Arrays.stream(net.predictOutputProbabilities(image)).max().isPresent();
            if (isPresent) {
                return Arrays.stream(net.predictOutputProbabilities(image)).max().getAsDouble() >
                        MINIMUM_CONFIDENCE;
            }
            throw new RuntimeException("Some error occurred. Please check output of the convolution network.");
        });
    }

    public static Filter<BoundingBox> createAverageWidthFilter(double averageWidth) {
        return new Filter<>(boundingBox -> boundingBox.getWidth() < averageWidth * AVERAGE_WIDTH_SCALING_FACTOR);

    }
}
