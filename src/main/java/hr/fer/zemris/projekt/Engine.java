package hr.fer.zemris.projekt;

import hr.fer.zemris.projekt.image.border.BorderImage;
import hr.fer.zemris.projekt.image.IImageFilter;
import hr.fer.zemris.projekt.image.ImageTransformer;
import hr.fer.zemris.projekt.image.binarization.OtsuBinarization;
import hr.fer.zemris.projekt.image.dilation.BinaryDilationFilter;
import hr.fer.zemris.projekt.image.grayscale.BT709GrayscaleFilter;
import hr.fer.zemris.projekt.image.interpolation.NearestNeighborInterpolation;
import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.image.models.Point;
import hr.fer.zemris.projekt.image.segmentation.ImageSegmentation;
import hr.fer.zemris.projekt.image.thinning.ZhangSeunThinningFilter;
import hr.fer.zemris.projekt.neural.NetworkLearner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Engine {

    private static final Path inputImagesDirectory = Paths.get("src/main/resources/digits_transformed/learning/0/");
    private static final Path outputDirectory = Paths.get("src/main/resources/orgi_output2/");


    public static void main(String[] args) throws IOException {
        try {
//            NetworkLearner learner = new NetworkLearner(28, 28,
//                    1, 10, 54, 100, 100_000_000);
//            learner.train();
//            for(int i=0; i < 10 ; i++) {
//                tranformImagesToBinary(Paths.get(inputImagesDirectory.toString(), String.valueOf(i)),
//                        Paths.get(outputDirectory.toString(), String.valueOf(i)));
//            }

            //saveDigitsFromImages(inputImagesDirectory, outputDirectory);

            tranformImagesToBinary(inputImagesDirectory, outputDirectory);

        } catch (IOException e) {

        }
    }


    public static void saveDigitsFromImages(Path inputDirectory, Path outputDirectory) throws IOException {
        File dir = new File(inputDirectory.toString());
        File[] directoryListing = dir.listFiles();
        ImageTransformer transformer = ImageTransformer.getInstance();

        if (directoryListing != null) {
            for (File child : directoryListing) {
                BufferedImage img = ImageIO.read(child);
                StringBuilder nameOfImage = new StringBuilder(child.getName());
                nameOfImage = new StringBuilder(nameOfImage.substring(0, nameOfImage.indexOf(".")));

                List<IImageFilter> imageFilters = new ArrayList<>();
                imageFilters.add(new BT709GrayscaleFilter());
                imageFilters.add(new OtsuBinarization());

                BufferedImage gray = transformer.transform(img, imageFilters);
                ImageSegmentation segmentation = ImageSegmentation.getInstance();
                List<List<Point>> points = segmentation.getContours(gray);
                List<BoundingBox> boundingBoxes = segmentation.getBoundingBoxAroundDigits(points);
                List<BufferedImage> digitsFromImage = segmentation.getImagesAroundBoundingBoxes(img, boundingBoxes);
                int counter = 0;
                String finalNameOfImage;
                for (BufferedImage bufferedImage : digitsFromImage) {
                    finalNameOfImage = nameOfImage + "_" + counter++ + ".png";
                    Path newImage = Paths.get(outputDirectory.toString(), finalNameOfImage);
                    ImageIO.write(bufferedImage, "png", new File(newImage.toString()));
                }
            }
        }
    }

    public static void tranformImagesToBinary(Path inputDirectory, Path outputDirectory) throws IOException {
        File dir = new File(inputDirectory.toString());
        File[] directoryListing = dir.listFiles();
        ImageTransformer transformer = ImageTransformer.getInstance();


        List<IImageFilter> imageFilters = new ArrayList<>();
//        imageFilters.add(new BT709GrayscaleFilter());
//        imageFilters.add(new OtsuBinarization());
//        imageFilters.add(new BorderImage());
//        imageFilters.add(new BinaryDilationFilter());
//        imageFilters.add(new NearestNeighborInterpolation(28, 28));
//     //   imageFilters.add(new CentreOfMassTranslation());
        imageFilters.add(new ZhangSeunThinningFilter());


        if (directoryListing != null) {
            for (File child : directoryListing) {
                BufferedImage img = ImageIO.read(child);
                String nameOfImage = child.getName();
                Path path = Paths.get(outputDirectory.toString(), nameOfImage);
                BufferedImage gray = transformer.transform(img, imageFilters);
                ImageIO.write(gray, "png", path.toFile());
            }
        }
    }
}
