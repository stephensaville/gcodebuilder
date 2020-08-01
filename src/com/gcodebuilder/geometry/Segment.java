package com.gcodebuilder.geometry;

import javafx.geometry.Point2D;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Getter
public class Segment extends Line {
    private static final Logger log = LogManager.getLogger(Segment.class);

    private final Point2D to;
    private final Point2D vector;
    private final double length;

    public static Segment of(Point2D from, Point2D to) {
        return new Segment(from, to, to.subtract(from));
    }

    public static Segment of(double fromX, double fromY, double toX, double toY) {
        return of(new Point2D(fromX, fromY), new Point2D(toX, toY));
    }

    private Segment(Point2D from, Point2D to, Point2D vector) {
        super(from, UnitVector.from(vector));
        this.to = to;
        this.vector = vector;
        this.length = vector.magnitude();
    }

    public Point2D intersect(Segment other) {
        Point2D between = getTo().subtract(other.getTo());

        double denominator = Math2D.det(getVector(), other.getVector());
        double thisParam = Math2D.det(between, other.getVector()) / denominator;
        double otherParam = Math2D.det(getVector(), between) / denominator;

        Point2D intersectionPoint = null;

        if (thisParam >= 0 && thisParam <= 1 &&
                otherParam >= -1 && otherParam <= 0) {
            intersectionPoint = getTo().add(getVector().multiply(-thisParam));
        }

        return intersectionPoint;
    }

    @Override
    public String toString() {
        return String.format("Segment(from=(%f,%f), to=(%f,%f))",
                getFrom().getX(), getFrom().getY(), getTo().getX(), getTo().getY());
    }
}
