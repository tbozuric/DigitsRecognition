package hr.fer.zemris.projekt.image.models;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Point implements Comparable<Point> {
    private int x;
    private int y;


    public static Point create(int x, int y) {
        return new Point(x, y);
    }

    private Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Point sub(Point other) {
        return new Point(this.x - other.x, this.y - other.y);
    }

    public Point add(Point other) {
        return new Point(this.x + other.x, this.y + other.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return x == point.x &&
                y == point.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public int compareTo(@NotNull Point o) {
        int x1 = this.x;
        int x2 = o.getX();
        int y1 = this.y;
        int y2 = o.getY();

        int xCompare = Integer.compare(x1, x2);
        if (xCompare != 0) {
            return xCompare;
        } else {
            return Integer.compare(y1, y2);
        }

    }
}
