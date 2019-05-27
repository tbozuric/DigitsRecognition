package hr.fer.zemris.projekt.neural;

import hr.fer.zemris.projekt.gui.models.LabeledImageModel;
import hr.fer.zemris.projekt.neural.exceptions.RetrainNetworkException;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

public interface INetwork {

    void train() throws IOException, InterruptedException;

    void save();

    double[] predictOutputProbabilities(BufferedImage image);

    int predictOutput(BufferedImage image);

    void loadModel(File file) throws IOException;

    void retrain(Collection<LabeledImageModel> newData) throws RetrainNetworkException;

}
