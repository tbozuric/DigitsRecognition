package hr.fer.zemris.projekt.gui.models;

import hr.fer.zemris.projekt.image.models.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;

public class BoxPredictionViewModel implements Comparable<BoxPredictionViewModel> {
    private BoundingBox boundingBox;
    private int prediction;

    public BoxPredictionViewModel(BoundingBox boundingBox, int prediction) {
        this.boundingBox = boundingBox;
        this.prediction = prediction;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }

    public int getPrediction() {
        return prediction;
    }

    public void setPrediction(int prediction) {
        this.prediction = prediction;
    }


    public BoxPredictionViewModel deepCopy() {
        return new BoxPredictionViewModel(boundingBox.deepCopy(), prediction);
    }

    @Override
    public int compareTo(@NotNull BoxPredictionViewModel boxPredictionViewModel) {
        return Comparator.comparingInt(BoxPredictionViewModel::getPrediction)
                .thenComparing(x -> x.getBoundingBox().compareTo(boxPredictionViewModel.getBoundingBox()))
                .compare(this, boxPredictionViewModel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoxPredictionViewModel that = (BoxPredictionViewModel) o;
        return prediction == that.prediction &&
                Objects.equals(boundingBox, that.boundingBox);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boundingBox, prediction);
    }


    @Override
    public String toString() {
        return boundingBox.toString() + " - " + prediction;
    }
}
