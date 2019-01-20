package hr.fer.zemris.projekt.gui.models;

import hr.fer.zemris.projekt.image.models.BoundingBox;
import hr.fer.zemris.projekt.image.models.Point;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LabeledImageModel {
    private Path path;
    private List<BoundingBox> boundingBoxes;
    private List<Integer> classifications;
    private int selectedBox = -1;
    private BufferedImage image;

    public LabeledImageModel(Path path, List<BoundingBox> boundingBoxes, List<Integer> classifications) {
        this.path = path;
        this.boundingBoxes = boundingBoxes;
        this.classifications = classifications;

    }

    public LabeledImageModel(Path path) {
        this.path = path;
        this.boundingBoxes = new ArrayList<>();
        this.classifications = new ArrayList<>();

    }

    public LabeledImageModel() {
        this.boundingBoxes = new ArrayList<>();
        this.classifications = new ArrayList<>();
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public List<BoundingBox> getBoundingBoxes() {
        return boundingBoxes;
    }

    public void setBoundingBoxes(List<BoundingBox> boundingBoxes) {
        this.boundingBoxes = boundingBoxes;
    }

    public List<Integer> getClassifications() {
        return classifications;
    }

    public void setClassifications(List<Integer> classifications) {
        this.classifications = classifications;
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

        int size = boundingBoxes.size();
        for (int i = 0; i < size; i++) {
            upLeft = boundingBoxes.get(i).getUpLeft();
            width = boundingBoxes.get(i).getWidth();
            height = boundingBoxes.get(i).getHeight();
            sb.append("\t");
            sb.append(upLeft.getX()).append("\t").append(upLeft.getY()).append("\t");
            sb.append(width).append("\t").append(height).append("\t").append(classifications.get(i))
                    .append(System.getProperty("line.separator"));
        }
        return sb.toString();
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
}

