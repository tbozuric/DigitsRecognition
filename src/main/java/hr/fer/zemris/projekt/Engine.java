package hr.fer.zemris.projekt;

import hr.fer.zemris.projekt.neural.ConvNetClassifier;
import hr.fer.zemris.projekt.neural.INetwork;

import java.io.IOException;

public class Engine {

    private static final int HEIGHT = 28;
    private static final int WIDTH = 28;
    private static final int CHANNELS = 1;
    private static final int NUMBER_OF_OUTPUTS = 10;
    private static final int BATCH_SIZE = 54;
    private static final int NUMBER_OF_EPOCHS = 30;

    public static void main(String[] args) {


        try {
            INetwork learner = new ConvNetClassifier(HEIGHT, WIDTH,
                    CHANNELS, NUMBER_OF_OUTPUTS, BATCH_SIZE, NUMBER_OF_EPOCHS);
            learner.train();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
