package hr.fer.zemris.projekt.image.postprocessor;

import hr.fer.zemris.projekt.image.managers.ImageManager;
import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.image.models.Point;
import hr.fer.zemris.projekt.math.Entropy;
import hr.fer.zemris.projekt.neural.INetwork;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SlidingWindowAnalyzer {

    private SlidingWindowAnalyzer() {

    }

    public static List<Pair<BoundingBox, Integer>> analyze(INetwork net, BufferedImage image, BoundingBox box) {
        List<Triple<BoundingBox, Integer, Double>> result = new ArrayList<>();
        Entropy entropy = Entropy.getInstance();

        int[] slidingWindowSizes = new int[]{13, 15, 17};


        int imageHeight = box.getHeight();

        List<Double> perplexities;

        BufferedImage imageCopy = ImageManager.deepCopy(image);
        BoundingBox boxCopy = box.deepCopy();
        double initPerplexity = entropy.perplexity(net.predictOutputProbabilities(imageCopy));
        int initPrediction = net.predictOutput(imageCopy);

        List<List<Triple<BoundingBox, Integer, Double>>> results = new ArrayList<>();


        for (int slidingWindow : slidingWindowSizes) {

            int startPointX;
            double perplexity = initPerplexity;
            double minPerplexity = perplexity;
            int imageWidthWithMinPerplexity = imageCopy.getWidth();
            int prediction = initPrediction;
            perplexities = new ArrayList<>();

            int currentWidth = slidingWindow;
            box = boxCopy;
            image = imageCopy;
            result.clear();

            while (true) {
                int i = 0;
                int steps = image.getWidth() / slidingWindow;

                if (steps <= 1) {
                    box.setWarningColor(Color.RED);
                    result.add(Triple.of(box, prediction, minPerplexity));
                    break;
                }

                while ((i++) < steps) {

                    BufferedImage newImage = image.getSubimage(0, 0, currentWidth, imageHeight);
                    double newPerplexity = entropy.perplexity(net.predictOutputProbabilities(newImage));


                    perplexities.add(newPerplexity);

                    if (newPerplexity < minPerplexity) {
                        minPerplexity = newPerplexity;
                        prediction = net.predictOutput(newImage);
                        imageWidthWithMinPerplexity = newImage.getWidth();
                    }

                    currentWidth += slidingWindow;
                }

                if (minPerplexity == perplexity) {
                    box.setWarningColor(Color.RED);
                    result.add(Triple.of(box, prediction, minPerplexity));
                    break;
                }

                int index = perplexities.indexOf(minPerplexity);


                int toEnd = box.getWidth() - imageWidthWithMinPerplexity;

                if (toEnd < slidingWindow) {
                    BoundingBox newBox = new BoundingBox(Point.create(box.getUpLeft().getX(), box.getUpLeft().getY()),
                            imageWidthWithMinPerplexity + toEnd, imageHeight);
                    newBox.setWarningColor(Color.RED);
                    result.add(Triple.of(newBox, net.predictOutput(image), minPerplexity));
                    break;
                }

                BoundingBox newBox = new BoundingBox(Point.create(box.getUpLeft().getX(), box.getUpLeft().getY()),
                        imageWidthWithMinPerplexity, imageHeight);
                newBox.setWarningColor(Color.RED);
                result.add(Triple.of(newBox, prediction, minPerplexity));

                startPointX = (index + 1) * slidingWindow;

                box = new BoundingBox(Point.create(box.getUpLeft().getX() + startPointX, box.getUpLeft().getY()),
                        image.getWidth() - startPointX, imageHeight);

                image = image.getSubimage(startPointX, 0, image.getWidth() - startPointX, imageHeight);
                prediction = net.predictOutput(image);
                currentWidth = slidingWindow;
                perplexity = entropy.perplexity(net.predictOutputProbabilities(image));
                minPerplexity = perplexity;
                perplexities.clear();
            }
            results.add(result);
        }

        return findTotalMinimumPerplexity(results);
    }

    @NotNull
    private static List<Pair<BoundingBox, Integer>> findTotalMinimumPerplexity(List<List<Triple<BoundingBox, Integer, Double>>> results) {
        double minSum = Double.MAX_VALUE;
        int index = -1;
        int i = 0;

        for (List<Triple<BoundingBox, Integer, Double>> oneResult : results) {
            int expFunctionValue = i;
            double squaredSumOfPerplexities = oneResult.stream().mapToDouble(x -> Math.pow(x.getRight(), 2.0)
                    * Math.exp(-expFunctionValue)).sum();

            if (squaredSumOfPerplexities <= minSum) {
                minSum = squaredSumOfPerplexities;
                index = i;
            }
            i++;
        }
        return results.get(index).stream().map(x -> Pair.create(x.getLeft(), x.getMiddle())).collect(Collectors.toList());
    }

}
