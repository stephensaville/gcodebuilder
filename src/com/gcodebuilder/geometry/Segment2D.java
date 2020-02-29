package com.gcodebuilder.geometry;

import javafx.geometry.Point2D;
import lombok.Getter;

@Getter
public class Segment2D extends Line2D {
    private final Point2D to;
    private final double length;

    public static Segment2D of(Point2D from, Point2D to) {
        Point2D vector = to.subtract(from);
        return new Segment2D(from, to, UnitVector2D.from(vector), vector.magnitude());
    }

    public static Segment2D of(double fromX, double fromY, double toX, double toY) {
        return of(new Point2D(fromX, fromY), new Point2D(toX, toY));
    }

    private Segment2D(Point2D from, Point2D to, UnitVector2D direction, double length) {
        super(from, direction);
        this.to = to;
        this.length = length;
    }

    public boolean isProjectedPointInSegment(Point2D projectedPoint) {
        return ((projectedPoint.getX() < getFrom().getX()) != (projectedPoint.getX() < to.getX())) &&
                ((projectedPoint.getY() < getFrom().getY()) != (projectedPoint.getY() < to.getY()));
    }
}
