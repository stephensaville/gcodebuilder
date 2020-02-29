package com.gcodebuilder.geometry;

import javafx.geometry.Point2D;
import lombok.Data;

@Data
public class Line2D {
    private final Point2D from;
    private final UnitVector2D direction;

    public Line2D(Point2D from, UnitVector2D direction) {
        this.from = from;
        this.direction = direction;
    }

    public Line2D(Point2D from, Point2D to) {
        this(from, UnitVector2D.from(from, to));
    }

    public Line2D(double fromX, double fromY, double toX, double toY) {
        this(new Point2D(fromX, fromY), UnitVector2D.from(toX - fromX, toY - fromY));
    }

    public Point2D project(Point2D point) {
        Point2D vectorToPoint = point.subtract(from);
        double distToProjection = vectorToPoint.dotProduct(direction);
        return from.add(direction.multiply(distToProjection));
    }
}
