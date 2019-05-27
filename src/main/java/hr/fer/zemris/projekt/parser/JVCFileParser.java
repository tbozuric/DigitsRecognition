package hr.fer.zemris.projekt.parser;

import hr.fer.zemris.projekt.exceptions.FileFormatException;
import hr.fer.zemris.projekt.gui.models.BoxPredictionViewModel;
import hr.fer.zemris.projekt.gui.models.LabeledImageModel;
import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.image.models.Point;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class JVCFileParser {
    private static final int NUMBER_OF_CHUNKS_IN_LINE = 5;
    private static JVCFileParser parser = new JVCFileParser();


    private JVCFileParser() {

    }

    public static JVCFileParser getParserInstance() {
        return parser;
    }

    public Map<Path, LabeledImageModel> parse(Path file) throws IOException, FileFormatException {
        Map<Path, LabeledImageModel> classifiedImages = new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(file)) {
            String line;
            Path path;
            boolean readPath = false;
            Set<BoxPredictionViewModel> viewModels = new TreeSet<>();
            LabeledImageModel imageModel = new LabeledImageModel();

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.startsWith(">")) {
                    if (readPath) {
                        imageModel.setViewModels(viewModels);
                        classifiedImages.put(imageModel.getPath(), imageModel);
                        viewModels = new TreeSet<>();
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

                    BoundingBox box = new BoundingBox(Point.create(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])),
                            Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));

                    int classOfDigit = Integer.parseInt(parts[4]);
                    viewModels.add(new BoxPredictionViewModel(box, classOfDigit));

                }
            }

            imageModel.setViewModels(viewModels);
            classifiedImages.put(imageModel.getPath(), imageModel);
        }
        return classifiedImages;
    }
}
