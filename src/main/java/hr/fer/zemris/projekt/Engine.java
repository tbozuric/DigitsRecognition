package hr.fer.zemris.projekt;

import hr.fer.zemris.projekt.image.IImageFilter;
import hr.fer.zemris.projekt.image.ImageTransformer;
import hr.fer.zemris.projekt.image.binarization.OtsuBinarization;
import hr.fer.zemris.projekt.image.border.BorderImage;
import hr.fer.zemris.projekt.image.dilation.BinaryDilationFilter;
import hr.fer.zemris.projekt.image.grayscale.BT709GrayscaleFilter;
import hr.fer.zemris.projekt.image.interpolation.NearestNeighborInterpolation;
import hr.fer.zemris.projekt.image.segmentation.ConnectedComponent;
import hr.fer.zemris.projekt.image.translation.CentreOfMassTranslation;
import hr.fer.zemris.projekt.image.managers.ImageManager;
import hr.fer.zemris.projekt.neural.ConvNetClassifier;
import hr.fer.zemris.projekt.neural.INetwork;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Engine {

    private static final Path inputImagesDirectory = Paths.get("src/main/resources/outputs_transformed/9");
    private static final Path outputDirectory = Paths.get("src/main/resources/outputs_transformed/9_transformed");


    public static void main(String[] args) throws IOException {
        try {
            INetwork learner = new ConvNetClassifier(28, 28,
                    1, 10, 54, 30);
            learner.train();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
