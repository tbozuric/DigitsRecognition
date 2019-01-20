package hr.fer.zemris.projekt.gui.listeners;

import hr.fer.zemris.projekt.image.models.BoundingBox;

public interface IBoundingBoxModelChangeListener {
    void modelChanged(BoundingBox newBox);
}
