package hr.fer.zemris.projekt;

import hr.fer.zemris.projekt.image.IImageFilter;
import hr.fer.zemris.projekt.image.ImageTransformer;
import hr.fer.zemris.projekt.image.binarization.OtsuBinarization;
import hr.fer.zemris.projekt.image.border.BorderImage;
import hr.fer.zemris.projekt.image.dilation.BinaryDilationFilter;
import hr.fer.zemris.projekt.image.grayscale.BT709GrayscaleFilter;
import hr.fer.zemris.projekt.image.interpolation.NearestNeighborInterpolation;
import hr.fer.zemris.projekt.image.translation.CentreOfMassTranslation;
import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Classifier {
    private static final int NUMBER_OF_CHANNELS = 1;
    private static Classifier classifier = new Classifier();

    private static final int IMAGE_HEIGHT = 28;
    private static final int IMAGE_WIDTH = 28;
    private ImageTransformer transformer;
    private List<IImageFilter> imageFilters;

    private Classifier() {

        transformer = ImageTransformer.getInstance();
        imageFilters = new ArrayList<>();

        imageFilters = new ArrayList<>();
        imageFilters.add(new BT709GrayscaleFilter());
        imageFilters.add(new OtsuBinarization());
        imageFilters.add(new BorderImage());
        imageFilters.add(new BinaryDilationFilter());
        imageFilters.add(new NearestNeighborInterpolation(IMAGE_WIDTH, IMAGE_HEIGHT));
        imageFilters.add(new CentreOfMassTranslation());

    }

    public static Classifier getInstance() {
        return classifier;
    }

    private INDArray loadImage(MultiLayerNetwork net, BufferedImage image) throws IOException {
        Objects.requireNonNull(net, "Classifier must not be null!");
        Objects.requireNonNull(image, "Image must not be null!");


        BufferedImage binary = transformer.transform(image, imageFilters);

        NativeImageLoader imageLoader = new NativeImageLoader(IMAGE_HEIGHT, IMAGE_WIDTH, NUMBER_OF_CHANNELS);
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(binary, "png", arrayOutputStream);


        INDArray imageAsArray = imageLoader.asMatrix(new ByteArrayInputStream(arrayOutputStream.toByteArray()));
        DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
        scaler.transform(imageAsArray);
        return imageAsArray;
    }

    public int classify(MultiLayerNetwork net, BufferedImage image) throws IOException {
        INDArray imageAsArray = loadImage(net, image);
        return net.predict(imageAsArray)[0];
    }

    public double[] outputProbabilities(MultiLayerNetwork net, BufferedImage image) throws IOException {
        INDArray imageAsArray = loadImage(net, image);
        INDArray output = net.output(imageAsArray);

        return output.data().asDouble();
    }
}
