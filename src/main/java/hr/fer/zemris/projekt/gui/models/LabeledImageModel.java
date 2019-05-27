package hr.fer.zemris.projekt.gui.models;

import hr.fer.zemris.projekt.image.models.Point;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class LabeledImageModel {

    private Path path;

    private Set<BoxPredictionViewModel> viewModels;

    private int selectedBox = -1;

    private BufferedImage image;

    public LabeledImageModel(Path path, List<BoxPredictionViewModel> viewModels) {
        this.path = path;
    }

    public LabeledImageModel(Path path) {
        this.path = path;
        this.viewModels = new TreeSet<>();

    }

    public LabeledImageModel() {
        this.viewModels = new TreeSet<>();
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Set<BoxPredictionViewModel> getViewModels() {
        return viewModels;
    }

    public void setViewModels(Set<BoxPredictionViewModel> viewModels) {
        this.viewModels = viewModels;
    }

    public int getSelectedBox() {
        return selectedBox;
    }

    public void setSelectedBox(int selectedBox) {
        this.selectedBox = selectedBox;
    }

    public BufferedImage getImage() {
        if (image == null) {
            try {
                image = ImageIO.read(path.toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabeledImageModel that = (LabeledImageModel) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {

        return Objects.hash(path);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(">").append(path.toString()).append(System.getProperty("line.separator"));
        Point upLeft;
        int width;
        int height;

        for (BoxPredictionViewModel viewModel : viewModels) {
            upLeft = viewModel.getBoundingBox().getUpLeft();
            width = viewModel.getBoundingBox().getWidth();
            height = viewModel.getBoundingBox().getHeight();
            sb.append("\t");
            sb.append(upLeft.getX()).append("\t").append(upLeft.getY()).append("\t");
            sb.append(width).append("\t").append(height).append("\t").append(viewModel.getPrediction())
                    .append(System.getProperty("line.separator"));
        }
        return sb.toString();
    }
}

