package hr.fer.zemris.projekt.math;

import java.util.Objects;

public class Entropy {

    private static Entropy entropy = new Entropy();


    private Entropy() {

    }

    public static Entropy getInstance() {
        return entropy;
    }

    private static double Log2(double n) {
        return Math.log(n) / Math.log(2);
    }

    public double entropy(double[] inputDistribution) {
        Objects.requireNonNull(inputDistribution, "Input distribution must not be null!");

        double entropy = 0;
        for (double prob : inputDistribution) {
            entropy -= prob * Log2(prob);
        }
        return entropy;
    }

    public double perplexity(double[] inputDistribution) {
        return Math.pow(2, entropy(inputDistribution));

    }
}
