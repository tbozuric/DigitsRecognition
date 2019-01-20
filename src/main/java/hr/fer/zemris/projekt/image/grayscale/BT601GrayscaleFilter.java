package hr.fer.zemris.projekt.image.grayscale;

public class BT601GrayscaleFilter extends AbstractGrayscaleFilter {
    @Override
    protected int getGrayComponent(int red, int green, int blue) {
        return (int) (red * 0.299f + green * 0.587f + blue * 0.114f);
    }

    @Override
    public String getFilterName() {
        return "BT.601 grayscale filter";
    }
}
