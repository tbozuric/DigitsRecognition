package hr.fer.zemris.projekt.gui.listeners;

import hr.fer.zemris.projekt.gui.models.BoxPredictionViewModel;

public interface IBoundingBoxActionListener {

    void selectedForEdit(BoxPredictionViewModel model);

    void selected(BoxPredictionViewModel model);

    void movedOver(BoxPredictionViewModel model);
}
