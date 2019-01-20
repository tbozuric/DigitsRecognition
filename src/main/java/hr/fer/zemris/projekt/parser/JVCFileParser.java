package hr.fer.zemris.projekt.parser;

import hr.fer.zemris.projekt.exceptions.FileFormatException;
import hr.fer.zemris.projekt.gui.models.LabeledImageModel;
import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.image.models.Point;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JVCFileParser {
    private static final int NUMBER_OF_CHUNKS_IN_LINE = 5;
    private static JVCFileParser parser;


    private JVCFileParser() {

    }

    public static JVCFileParser getParserInstance() {
        if (parser == null) {
            parser = new JVCFileParser();
        }
        return parser;
    }

    public Map<Path, LabeledImageModel> parse(Path file) throws IOException, FileFormatException {
        Map<Path, LabeledImageModel> classifiedImages = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(file)) {
            String line;
            Path path;
            boolean readPath = false;
            List<BoundingBox> boundingBoxes = new ArrayList<>();
            List<Integer> classifications = new ArrayList<>();
            LabeledImageModel imageModel = new LabeledImageModel();
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith(">")) {
                    if (readPath) {
                        imageModel.setBoundingBoxes(new ArrayList<>(boundingBoxes));
                        imageModel.setClassifications(new ArrayList<>(classifications));
                        classifiedImages.put(imageModel.getPath(), imageModel);
                        classifications = new ArrayList<>();
                        boundingBoxes = new ArrayList<>();
                        imageModel = new LabeledImageModel();
                    }
                    line = line.substring(1);
                    path = Paths.get(line);
                    imageModel.setPath(path);
                    readPath = true;
                } else {
                    String[] parts = line.split("\t");
                    if (parts.length != NUMBER_OF_CHUNKS_IN_LINE) {
                        throw new FileFormatException("File is in illegal format.");
                    }
                    BoundingBox box = new BoundingBox(new Point(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])),
                            Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));

                    Integer classOfDigit = Integer.valueOf(parts[4]);
                    boundingBoxes.add(box);
                    classifications.add(classOfDigit);
                }
            }
            imageModel.setBoundingBoxes(new ArrayList<>(boundingBoxes));
            imageModel.setClassifications(new ArrayList<>(classifications));
            classifiedImages.put(imageModel.getPath(), imageModel);
        }
        return classifiedImages;
    }
}
