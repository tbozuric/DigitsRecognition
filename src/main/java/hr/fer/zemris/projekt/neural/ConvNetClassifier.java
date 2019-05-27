package hr.fer.zemris.projekt.neural;

import hr.fer.zemris.projekt.Classifier;
import hr.fer.zemris.projekt.gui.models.BoxPredictionViewModel;
import hr.fer.zemris.projekt.gui.models.LabeledImageModel;
import hr.fer.zemris.projekt.image.managers.ImageManager;
import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.neural.exceptions.RetrainNetworkException;
import org.datavec.api.io.labels.ParentPathLabelGenerator;
import org.datavec.api.split.FileSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.jetbrains.annotations.NotNull;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.dataset.api.preprocessor.Normalizer;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.primitives.Pair;
import org.nd4j.linalg.schedule.MapSchedule;
import org.nd4j.linalg.schedule.ScheduleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class ConvNetClassifier implements INetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConvNetClassifier.class);
    private static final String NETWORK_PATH = "src/main/resources/digits-model_extended_6.zip";


    private static final String trainImages = "src/main/resources/digits_transformed/learning/";
    private static final String testImages = "src/main/resources/digits_transformed/testing/";
    private static final int NUMBER_OF_RETRAIN_EPOCHS = 5;
    private static final int DEFAULT_HEIGHT = 28;
    private static final int DEFAULT_WIDTH = 28;
    private static final int DEFAULT_CHANNELS = 1;
    private static final int DEFAULT_OUTPUT_NUMBER = 10;
    private static final int DEFAULT_NUMBER_EPOCHS = 30;
    private static final int DEFAULT_BATCH_SIZE = 54;


    private int height;
    private int width;
    private int channels; // single channel for grayscale images
    private int outputNum; // 10 digits classification
    private int batchSize;
    private int nEpochs;
    private MultiLayerNetwork net;
    private DataNormalization scaler;

    private int seed = 1234;
    private Random randNumGen = new Random(seed);

    public ConvNetClassifier(File file) throws IOException {
        loadModel(file);
        this.height = DEFAULT_HEIGHT;
        this.width = DEFAULT_WIDTH;
        this.channels = DEFAULT_CHANNELS;
        this.outputNum = DEFAULT_OUTPUT_NUMBER;
        this.nEpochs = DEFAULT_NUMBER_EPOCHS;
        this.batchSize = DEFAULT_BATCH_SIZE;
    }


    public ConvNetClassifier(int height, int width, int channels, int outputNum, int batchSize, int numberOfEpochs) {
        this.height = height;
        this.width = width;
        this.channels = channels;
        this.outputNum = outputNum;
        this.batchSize = batchSize;
        this.nEpochs = numberOfEpochs;

    }

    public ConvNetClassifier(int height, int width, int channels, int outputNum, int batchSize, int numberOfEpochs,
                             MultiLayerNetwork net) {
        this.height = height;
        this.width = width;
        this.channels = channels;
        this.outputNum = outputNum;
        this.batchSize = batchSize;
        this.nEpochs = numberOfEpochs;
        this.net = net;
    }


    public void train() throws IOException {

        LOGGER.info("Data vectorization...");
        // vectorization of train data

        ParentPathLabelGenerator labelMaker = new ParentPathLabelGenerator(); // parent path as the image label

        DataSetIterator trainIter = getDataSetIterator(trainImages, labelMaker);

        // pixel values from 0-255 to 0-1 (min-max scaling)
        scaler = new ImagePreProcessingScaler(0, 1);
        scaler.fit(trainIter);
        trainIter.setPreProcessor(scaler);


        DataSetIterator testIter = getDataSetIterator(testImages, labelMaker);
        testIter.setPreProcessor(scaler); // same normalization for better results

        LOGGER.info("Network configuration and training...");
        if (net == null) {
            net = configAndGetMultiLayerNetwork();
        }


        net.init();
        net.setListeners(new ScoreIterationListener(100));


        // Visualizing Network Training
        UIServer uiServer = UIServer.getInstance();
        StatsStorage statsStorage = new InMemoryStatsStorage();
        uiServer.attach(statsStorage);
        net.setListeners(new StatsListener(statsStorage), new ScoreIterationListener(100));

        //Then add the StatsListener to collect this information from the network, as it trains
        //net.setListeners(new StatsListener(statsStorage));

        LOGGER.info("Total numbers of parameters: " + net.numParams());

        // evaluation while training (the score should go down)
        for (int i = 0; i < nEpochs; i++) {
            net.fit(trainIter);
            LOGGER.info("Completed epoch {}", i);

            Evaluation eval = net.evaluate(testIter);
            LOGGER.info(eval.stats());

            trainIter.reset();
            testIter.reset();
        }

        save();
    }

    @Override
    public void save() {
        try {
            ModelSerializer.writeModel(net, new File("src/main/resources/digits-model_extended_7.zip"),
                    true, scaler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public double[] predictOutputProbabilities(BufferedImage image) {
        try {
            if (net == null) {
                net = ModelSerializer.restoreMultiLayerNetwork(new File(NETWORK_PATH));
            }
            Classifier classifier = Classifier.getInstance();
            return classifier.outputProbabilities(net, image);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public int predictOutput(BufferedImage image) {
        try {
            if (net == null) {
                loadModel(new File(NETWORK_PATH));
            }
            Classifier classifier = Classifier.getInstance();
            return classifier.classify(net, image);

        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public void loadModel(File file) throws IOException {
        Pair<MultiLayerNetwork, Normalizer> data = ModelSerializer.restoreMultiLayerNetworkAndNormalizer(file, true);
        net = data.getFirst();
        scaler = (DataNormalization) data.getValue();
        System.out.println(net.score());
    }

    @Override
    public void retrain(Collection<LabeledImageModel> newData) throws RetrainNetworkException {

        try {
            Path tempInputImages = Files.createTempDirectory("training");

            for (int i = 0; i < 10; i++) {
                File digitDirectory = new File(tempInputImages.toFile().toString() + "/" + i);
                if (!digitDirectory.mkdirs()) {
                    throw new RuntimeException("Some error occurred while creating directories.");
                }
            }

            int counter = 0;
            Set<Integer> numOfPossibleOutputs = new HashSet<>();
            for (LabeledImageModel model : newData) {
                BufferedImage image = model.getImage();
                for (BoxPredictionViewModel viewModel : model.getViewModels()) {
                    BoundingBox box = viewModel.getBoundingBox();
                    int prediction = viewModel.getPrediction();
                    numOfPossibleOutputs.add(prediction);

                    BufferedImage subImage = image.getSubimage(box.getUpLeft().getX(), box.getUpLeft().getY(),
                            box.getWidth(), box.getHeight());

                    String finalNameOfImage = (counter++) + "_" + prediction + ".png";

                    Path newImage = Paths.get(tempInputImages.toString(), String.valueOf(prediction), finalNameOfImage);
                    ImageIO.write(ImageManager.transformImageToBinary(subImage, width, height),
                            "png", new File(newImage.toString()));


                }

            }

            if (net == null) {
                loadModel(new File(NETWORK_PATH));
            }

            if (numOfPossibleOutputs.size() != 10) {
                throw new RetrainNetworkException("Please label at least one digit for each class.");
            }

            DataSetIterator trainIter = getDataSetIterator(tempInputImages.toString(), new ParentPathLabelGenerator());
            trainIter.setPreProcessor(scaler);

            for (int i = 0; i < NUMBER_OF_RETRAIN_EPOCHS; i++) {
                net.fit(trainIter);
                trainIter.reset();
            }
        } catch (IOException e) {
            throw new RetrainNetworkException("Some error occurred. Please try again.");
        }
    }

    @NotNull
    private DataSetIterator getDataSetIterator(String path, ParentPathLabelGenerator labelMarker) throws IOException {
        File data = new File(path);
        FileSplit dataSplit = new FileSplit(data, NativeImageLoader.ALLOWED_FORMATS, randNumGen);

        ImageRecordReader recordReader = new ImageRecordReader(height, width, channels, labelMarker);
        recordReader.initialize(dataSplit);

        return new RecordReaderDataSetIterator(recordReader, batchSize, 1, outputNum);
    }


    private MultiLayerNetwork configAndGetMultiLayerNetwork() {
        Map<Integer, Double> lrSchedule = new HashMap<>();
        lrSchedule.put(0, 0.06); // iteration #, learning rate
        lrSchedule.put(200, 0.05);
        lrSchedule.put(600, 0.028);
        lrSchedule.put(800, 0.0060);
        lrSchedule.put(1000, 0.001);

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .l2(0.0005)
                .updater(new Nesterovs(new MapSchedule(ScheduleType.ITERATION, lrSchedule)))
                .weightInit(WeightInit.XAVIER)
                .list()
                .layer(0, new ConvolutionLayer.Builder(5, 5)
                        .nIn(channels)
                        .stride(1, 1)
                        .nOut(20)
                        .activation(Activation.IDENTITY)
                        .build())
                .layer(1, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(2, 2)
                        .stride(2, 2)
                        .build())
                .layer(2, new ConvolutionLayer.Builder(5, 5)
                        .stride(1, 1) // nIn need not specified in later layers
                        .nOut(200)
                        .activation(Activation.RELU)
                        .build())
                .layer(3, new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX)
                        .kernelSize(2, 2)
                        .stride(2, 2)
                        .build())
                .layer(4, new DenseLayer.Builder().activation(Activation.RELU)
                        .nOut(400).build())
                .layer(5, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                        .nOut(outputNum)
                        .activation(Activation.SOFTMAX)
                        .build())
                .setInputType(InputType.convolutionalFlat(28, 28, 1)) // InputType.convolutional for normal image
                .backpropType(BackpropType.Standard).build();

        return new MultiLayerNetwork(conf);
    }
}
