package hr.fer.zemris.projekt.image.models;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Comparator;
import java.util.Objects;

public class BoundingBox implements Comparable<BoundingBox> {

    private Point upLeft;
    private int width;
    private int height;
    private Color warningColor;
    private Color groupColor;


    public BoundingBox(Point upLeft, int width, int height) {
        this.upLeft = upLeft;
        this.width = width;
        this.height = height;
    }

    public Point getUpLeft() {
        return upLeft;
    }

    public void setUpLeft(Point upLeft) {
        this.upLeft = upLeft;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setWarningColor(Color warningColor) {
        this.warningColor = warningColor;
    }

    public Color getWarningColor() {
        return warningColor;
    }

    public Color getGroupColor() {
        return groupColor;
    }

    public void setGroupColor(Color groupColor) {
        this.groupColor = groupColor;
    }

    public double minimumDistanceToBox(BoundingBox b2) {

        BoundingBox outerBox = getOuterBox(this, b2);

        int innerWidth = Math.max(0, outerBox.getWidth() - this.getWidth() - b2.getWidth());
        int innerHeight = Math.max(0, outerBox.getHeight() - this.getHeight() - b2.getHeight());

        return Math.sqrt(Math.pow(innerWidth, 2.0) + Math.pow(innerHeight, 2.0));
    }

    public boolean isInsideBox(BoundingBox other) {
        Point otherUpLeft = other.getUpLeft();
        Point otherDownRight = Point.create(otherUpLeft.getX() + other.getWidth(), otherUpLeft.getY() + other.getHeight());
        return otherUpLeft.getX() <= upLeft.getX() && otherUpLeft.getY() <= upLeft.getY()
                && otherDownRight.getX() >= (upLeft.getX() + width) && otherDownRight.getY() >= (upLeft.getY() + height);
    }

    public static BoundingBox getOuterBox(BoundingBox b1, BoundingBox b2) {
        Point upLeftFirstBox = b1.getUpLeft();
        Point upLeftSecondBox = b2.getUpLeft();

        int X1 = upLeftFirstBox.getX();
        int X2 = upLeftSecondBox.getX();

        int Y1 = upLeftFirstBox.getY();
        int Y2 = upLeftSecondBox.getY();

        int outerBoxXUpLeft = Math.min(X1, X2);
        int outerBoxYUpLeft = Math.min(Y1, Y2);

        int outerBoxWidth = Math.max(X1 + b1.getWidth(), X2 + b2.getWidth())
                - outerBoxXUpLeft;
        int outerBoxHeight = Math.max(Y1 + b1.getHeight(), Y2 + b2.getHeight())
                - outerBoxYUpLeft;

        return new BoundingBox(Point.create(outerBoxXUpLeft, outerBoxYUpLeft), outerBoxWidth, outerBoxHeight);
    }

    public BoundingBox deepCopy() {
        return new BoundingBox(Point.create(upLeft.getX(), upLeft.getY()), width, height);
    }


    @Override
    public String toString() {
        return "(" + upLeft.getX() + ", " + upLeft.getY() + ") - (" + (upLeft.getX() + width) + ", "
                + (upLeft.getY() + height) + ")";
    }

    @Override
    public int compareTo(@NotNull BoundingBox o) {
        return Comparator.comparing(BoundingBox::getUpLeft).compare(this, o);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoundingBox that = (BoundingBox) o;
        return width == that.width &&
                height == that.height &&
                Objects.equals(upLeft, that.upLeft);
    }

    @Override
    public int hashCode() {
        return Objects.hash(upLeft, width, height);
    }


}
