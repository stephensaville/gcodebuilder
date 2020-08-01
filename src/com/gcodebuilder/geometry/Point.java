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

    public Point add(Point2D point2D) {
        return new Point(this.point2D.add(point2D));
    }
}
