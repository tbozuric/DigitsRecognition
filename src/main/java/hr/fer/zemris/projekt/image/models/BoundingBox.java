package hr.fer.zemris.projekt.image.models;

import org.jetbrains.annotations.NotNull;

public class BoundingBox implements Comparable<BoundingBox> {

    private Point upLeft;
    private int width;
    private int height;


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

    @Override
    public String toString() {
        return "(" + upLeft.getX() + ", " + upLeft.getY() + ") - (" + (upLeft.getX() + width) + ", "
                + (upLeft.getY() + height) + ")";
    }

    @Override
    public int compareTo(@NotNull BoundingBox o) {
        return Integer.compare(upLeft.getX(), o.upLeft.getX());
    }
}
