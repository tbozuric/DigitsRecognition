package hr.fer.zemris.projekt;

import hr.fer.zemris.projekt.neural.ConvNetClassifier;
import hr.fer.zemris.projekt.neural.INetwork;

import java.io.IOException;

public class Engine {

    public static void main(String[] args) {
        try {
            INetwork learner = new ConvNetClassifier(28, 28,
                    1, 10, 54, 30);
            learner.train();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
