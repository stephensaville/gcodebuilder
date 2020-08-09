package com.gcodebuilder.geometry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.geometry.Point2D;

public class Point {
    private final Point2D point2D;

    @JsonCreator
    public Point(@JsonProperty("x") double x,
                 @JsonProperty("y") double y) {
        this.point2D = new Point2D(x, y);
    }

    public Point(Point2D point2D) {
        this.point2D = point2D;
    }

    public Point2D asPoint2D() {
        return point2D;
    }

    @JsonProperty
    public double getX() {
        return point2D.getX();
    }

    @JsonProperty
    public double getY() {
        return point2D.getY();
    }

    public double distance(Point2D point2D) {
        return this.point2D.distance(point2D);
    }

    public double distance(Point otherPoint) {
        return distance(otherPoint.asPoint2D());
    }

    public Point add(Point2D point2D) {
        return new Point(this.point2D.add(point2D));
    }

    public boolean isSame(Point otherPoint, double maxDistance) {
        if (otherPoint == null) {
            return false;
        } else if (this.equals(otherPoint)) {
            return true;
        } else {
            return this.distance(otherPoint) < maxDistance;
        }
    }

    public boolean isSame(Point2D point2D, double maxDistance) {
        if (point2D == null) {
            return false;
        } else {
            return this.distance(point2D) < maxDistance;
        }
    }

    @Override
    public String toString() {
        return String.format("Point(%f, %f)", point2D.getX(), point2D.getY());
    }
}

