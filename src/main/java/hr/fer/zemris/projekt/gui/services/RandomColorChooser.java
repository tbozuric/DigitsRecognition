package hr.fer.zemris.projekt.gui.services;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RandomColorChooser {

    private static final Random rand = new Random();
    private static final Map<Integer, Color> predictionColors = new HashMap<>();

    static {
        for (int i = 0; i <= 9; i++) {
            predictionColors.put(i, getRandomColor());
        }
    }

    public static Color getRandomColor() {
        float red = rand.nextFloat();
        float green = rand.nextFloat();
        float blue = rand.nextFloat();
        float alpha = 0.5f;

        return new Color(red, green, blue, alpha);
    }

    public static Color getColorForPrediction(int prediction) {
        return predictionColors.get(prediction);
    }
}
