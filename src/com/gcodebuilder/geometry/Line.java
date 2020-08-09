package com.gcodebuilder.geometry;

import javafx.geometry.Point2D;
import lombok.Data;

@Data
public class Line {
    private final Point2D from;
    private final UnitVector direction;

    public Line(Point2D from, UnitVector direction) {
        this.from = from;
        this.direction = direction;
    }

    public Line(Point2D from, Point2D to) {
        this(from, UnitVector.from(from, to));
    }

    public Line(double fromX, double fromY, double toX, double toY) {
        this(new Point2D(fromX, fromY), UnitVector.from(toX - fromX, toY - fromY));
    }

    public Point2D project(Point2D point) {
        Point2D vectorToPoint = point.subtract(from);
        double distToProjection = vectorToPoint.dotProduct(direction);
        return from.add(direction.multiply(distToProjection));
    }

    public double getAngle() {
        return getDirection().getAngle();
    }

    @Override
    public String toString() {
        return String.format("Line(from=(%f,%f), direction=(%f,%f))",
                getFrom().getY(), getFrom().getY(), getDirection().getX(), getDirection().getY());
    }
}
