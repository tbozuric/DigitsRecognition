package hr.fer.zemris.projekt.neural;

import hr.fer.zemris.projekt.gui.models.LabeledImageModel;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.datavec.api.io.filters.RandomPathFilter;
import org.datavec.api.records.metadata.RecordMetaDataImageURI;
import org.datavec.api.split.FileSplit;
import org.datavec.api.split.InputSplit;
import org.datavec.image.loader.NativeImageLoader;
import org.datavec.image.recordreader.objdetect.ObjectDetectionRecordReader;
import org.datavec.image.recordreader.objdetect.impl.VocLabelProvider;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.objdetect.Yolo2OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.transferlearning.FineTuneConfiguration;
import org.deeplearning4j.nn.transferlearning.TransferLearning;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.deeplearning4j.zoo.model.TinyYOLO;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.RmsProp;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class YouOnlyLookOnceNetwork extends AbstractNetworkLearner {

    private static final int GRID_WIDTH = 13;
    private static final int GRID_HEIGHT = 13;
    private static final int BOXES_NUMBER = 5;
    private static final double[][] PRIOR_BOXES = {{1.5, 1.5}, {2, 2}, {3, 3}, {3.5, 8}, {4, 9}};//anchors boxes


    /*parent Dataset folder "DATA_DIR" contains two subfolder "images" and "annotations" */
    private static final String DATA_DIR = "/home/tomo/Desktop/FER/dipl_druga_god/dipl_rad/database/processed";

    /* Yolo loss function prameters for more info
    https://stats.stackexchange.com/questions/287486/yolo-loss-function-explanation*/
    private static final double LAMDBA_COORD = 1.0;
    private static final double LAMDBA_NO_OBJECT = 0.5;

    public YouOnlyLookOnceNetwork(int inputWidth, int inputHeight, int channels, int batchSize,
                                  int epochs, int learningRate, int seed, int numberOfClasses) {
        super(inputWidth, inputHeight, channels, batchSize, epochs, learningRate, seed, numberOfClasses);
    }

    @Override
    public void train() throws IOException, InterruptedException {
        Random rng = new Random(seed);

        //Initialize the user interface backend, it is just as tensorboard.
        //it starts at http://localhost:9000
        UIServer uiServer = UIServer.getInstance();

        //Configure where the network information (gradients, score vs. time etc) is to be stored. Here: store in memory.
        StatsStorage statsStorage = new InMemoryStatsStorage();

        //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
        uiServer.attach(statsStorage);

        File imageDir = new File(DATA_DIR, "images");

        System.out.println("Load data...");
        RandomPathFilter pathFilter = new RandomPathFilter(rng) {
            @Override
            protected boolean accept(String name) {
                name = name.replace("/images/", "/annotations/").replace(".jpg", ".xml");
                //System.out.println("Name " + name);
                try {
                    return new File(new URI(name)).exists();
                } catch (URISyntaxException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };

        InputSplit[] data = new FileSplit(imageDir, NativeImageLoader.ALLOWED_FORMATS, rng).sample(pathFilter, 0.9, 0.1);
        InputSplit trainData = data[0];
        InputSplit testData = data[1];

        ObjectDetectionRecordReader recordReaderTrain = new ObjectDetectionRecordReader(inputHeight, inputWidth, channels,
                GRID_HEIGHT, GRID_WIDTH, new VocLabelProvider(DATA_DIR));
        recordReaderTrain.initialize(trainData);

        ObjectDetectionRecordReader recordReaderTest = new ObjectDetectionRecordReader(inputHeight, inputWidth, channels,
                GRID_HEIGHT, GRID_WIDTH, new VocLabelProvider(DATA_DIR));
        recordReaderTest.initialize(testData);

        RecordReaderDataSetIterator train = new RecordReaderDataSetIterator(recordReaderTrain, batchSize, 1, 1, true);
        train.setPreProcessor(new ImagePreProcessingScaler(0, 1));

        RecordReaderDataSetIterator test = new RecordReaderDataSetIterator(recordReaderTest, 1, 1, 1, true);
        test.setPreProcessor(new ImagePreProcessingScaler(0, 1));

        ComputationGraph pretrained = (ComputationGraph) TinyYOLO.builder().build().initPretrained();

        INDArray priors = Nd4j.create(PRIOR_BOXES);
        FineTuneConfiguration fineTuneConf = new FineTuneConfiguration.Builder()
                .seed(seed)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .gradientNormalization(GradientNormalization.RenormalizeL2PerLayer)
                .gradientNormalizationThreshold(1.0)
                .updater(new RmsProp(learningRate))
                .activation(Activation.IDENTITY).miniBatch(true)
                .trainingWorkspaceMode(WorkspaceMode.ENABLED)
                .build();

        ComputationGraph model = new TransferLearning.GraphBuilder(pretrained)
                .fineTuneConfiguration(fineTuneConf)
                .setInputTypes(InputType.convolutional(inputHeight, inputWidth, channels))
                .removeVertexKeepConnections("conv2d_9")
                .addLayer("convolution2d_9",
                        new ConvolutionLayer.Builder(1, 1)
                                .nIn(1024)
                                .nOut(BOXES_NUMBER * (5 + classesNumber))
                                .stride(1, 1)
                                .convolutionMode(ConvolutionMode.Same)
                                .weightInit(WeightInit.UNIFORM)
                                .hasBias(false)
                                .activation(Activation.IDENTITY)
                                .build(), "leaky_re_lu_8")
                .addLayer("outputs",
                        new Yolo2OutputLayer.Builder()
                                .lambbaNoObj(LAMDBA_NO_OBJECT)
                                .lambdaCoord(LAMDBA_COORD)
                                .boundingBoxPriors(priors)
                                .build(), "convolution2d_9")
                .setOutputs("outputs")
                .build();

        System.out.println("\n Model Summary \n" + model.summary());

        System.out.println("Train model...");
        //model.setListeners(new ScoreIterationListener(1));//print score after each iteration on stout
        model.setListeners(new StatsListener(statsStorage));// visit http://localhost:9000 to track the training process
        for (int i = 0; i < epochs; i++) {
            train.reset();
            while (train.hasNext()) {
                model.fit(train.next());
            }
            System.out.println("*** Completed epoch" + i + "***");
        }

        System.out.println("*** Saving Model ***");
        ModelSerializer.writeModel(model, "model.data", true);
        System.out.println("*** Training Done ***");


        //visualize results on the test set, Just hit any key in your keyboard to iterate the test set.
        System.out.println("*** Visualizing model on test data ***");
        YoloModel detector = new YoloModel();
        CanvasFrame mainframe = new CanvasFrame("Rubix Cube");
        mainframe.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        mainframe.setCanvasSize(600, 600);
        mainframe.setLocationRelativeTo(null);
        mainframe.setVisible(true);

        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        test.setCollectMetaData(true);
        while (test.hasNext() && mainframe.isVisible()) {
            org.nd4j.linalg.dataset.DataSet ds = test.next();
            RecordMetaDataImageURI metadata = (RecordMetaDataImageURI) ds.getExampleMetaData().get(0);
            opencv_core.Mat image = opencv_imgcodecs.imread(metadata.getURI().getPath().substring(1));
            //System.out.println("Path: " +metadata.getURI().getPath());
            detector.detectRubixCube(ds.getFeatures(), image, 0.4);
            mainframe.setTitle(new File(metadata.getURI()).getName());
            mainframe.showImage(converter.convert(image));
            mainframe.waitKey();
        }
        mainframe.dispose();
        System.exit(0);
    }


    @Override
    public void save() {

    }

    @Override
    public double[] predictOutputProbabilities(BufferedImage image) {
        return new double[0];
    }

    @Override
    public int predictOutput(BufferedImage image) {
        return 0;
    }

    @Override
    public void loadModel(File file) throws IOException {

    }

    @Override
    public void retrain(Collection<LabeledImageModel> newData) {

    }
}
