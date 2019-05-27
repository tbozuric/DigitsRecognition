package hr.fer.zemris.projekt.neural;

public abstract class AbstractNetworkLearner implements INetwork {

    static int inputWidth;
    static int inputHeight;
    static int channels;

    static int batchSize;
    static int epochs;
    static double learningRate;
    static int seed;

    static int classesNumber;


    public AbstractNetworkLearner(int inputWidth, int inputHeight, int channels,
                                  int batchSize, int epochs, int learningRate, int seed, int numberOfClasses) {
        AbstractNetworkLearner.inputWidth = inputWidth;
        AbstractNetworkLearner.inputHeight = inputHeight;
        AbstractNetworkLearner.channels = channels;
        AbstractNetworkLearner.batchSize = batchSize;
        AbstractNetworkLearner.epochs = epochs;
        AbstractNetworkLearner.learningRate = learningRate;
        AbstractNetworkLearner.seed = seed;
        classesNumber = numberOfClasses;
    }


}
