package hr.fer.zemris.projekt.image.grayscale;

public class AveragingGrayscaleFilter extends AbstractGrayscaleFilter {


    @Override
    protected int getGrayComponent(int red, int green, int blue) {
        return (int) ((red + green + blue) / 3.0f);
    }

    @Override
    public String getFilterName() {
        return "Averaging grayscale filter";
    }
}
