package hr.fer.zemris.projekt.image.grayscale;

public class BT709GrayscaleFilter extends AbstractGrayscaleFilter {
    @Override
    protected int getGrayComponent(int red, int green, int blue) {
        return (int)(red * 0.2126f + green * 0.7152f + blue * 0.0722f);
    }

    @Override
    public String getFilterName() {
        return "BT.709 grayscale filter";
    }
}
