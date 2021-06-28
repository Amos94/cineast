package org.vitrivr.cineast.core.mms.Helper;

import java.util.Objects;

public final class Point implements Comparable<Point> {

    public final double x;
    public final double y;
    public double label;


    public Point(double x, double y) {
        this.x = x;
        this.y = y;
        this.label = 0;
    }

    public Point(double x, double y, double label) {
        this.x = x;
        this.y = y;
        this.label = label;
    }


    public String toString() {
        return String.format("Point(%g, %g) -> label: %g", x, y, label);
    }


    public boolean equals(Object obj) {
        if (!(obj instanceof Point))
            return false;
        else {
            Point other = (Point)obj;
            return x == other.x && y == other.y;
        }
    }


    public int hashCode() {
        return Objects.hash(x, y, label);
    }


    public int compareTo(Point other) {
        if (x != other.x)
            return Double.compare(x, other.x);
        else
            return Double.compare(y, other.y);
    }

}
