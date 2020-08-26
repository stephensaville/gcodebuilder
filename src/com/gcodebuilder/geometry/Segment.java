package com.gcodebuilder.geometry;

import javafx.geometry.Point2D;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.MalformedLinkException;
import java.util.List;

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

    private Segment(Point2D from, Point2D to, Point2D vector, UnitVector direction, double length) {
        super(from, direction);
        this.to = to;
        this.vector = vector;
        this.length = length;
    }

    public Segment move(Point2D offset) {
        return new Segment(getFrom().add(offset), getTo().add(offset), getVector(), getDirection(), getLength());
    }

    public Segment flip() {
        return new Segment(getTo(), getFrom(), getVector().multiply(-1), getDirection().invert(), getLength());
    }

    public Point2D intersect(Segment other, boolean allowOutside) {
        Point2D between = getTo().subtract(other.getTo());

        double denominator = Math2D.det(getVector(), other.getVector());
        double thisParam = Math2D.det(between, other.getVector()) / denominator;

        boolean allowed = allowOutside;
        if (!allowed) {
            double otherParam = Math2D.det(getVector(), between) / denominator;

            allowed = thisParam > 0 && thisParam < 1 && otherParam > -1 && otherParam < 0;
        }

        if (allowed) {
            return getTo().add(getVector().multiply(-thisParam));
        } else {
            return null;
        }
    }

    public Point2D intersect(Segment other) {
        return intersect(other, false);
    }

    public Point2D project(Point2D point) {
        Point2D vectorToPoint = point.subtract(getFrom());
        double distToProjection = vectorToPoint.dotProduct(getDirection());
        if (distToProjection >= 0 && distToProjection <= length) {
            return getFrom().add(getDirection().multiply(distToProjection));
        } else {
            return null;
        }
    }

    /**
     * Returns true if the horizontal line passing through point intersects this segment, and the x coordinate of
     * point is less than the x coordinate of the intersection point. In other words, if point is to the left of
     * the intersection point in the typical right-handed cartesian coordinate system. This function can be used
     * to build an algorithm to check if a point is inside a path composed of segments using an even-odd winding
     * rule that flips between even and odd when this function returns true.
     *
     * @param point a point
     * @return true if rule should flip between even and odd, false if not
     */
    public boolean isWindingMatch(Point2D point) {
        return (point.getY() < getFrom().getY() != point.getY() < getTo().getY()) &&
                point.getX() < calculateX(point.getY());
    }

    /**
     * Checks if a point is inside the path defined by a list of segments. This function assumes the path is closed,
     * but does not check if the path is closed, so this function should only be used with paths known to be closed.
     *
     * @param path path defined by a list of segments
     * @param point a point
     * @return true if point is inside the path, false if point is outside the path
     */
    public static boolean isPointInsidePath(List<Segment> path, Point2D point) {
        boolean pointInPath = false;
        for (Segment segment : path) {
            if (segment.isWindingMatch(point)) {
                pointInPath = !pointInPath;
            }
        }
        return pointInPath;
    }

    @Override
    public String toString() {
        return String.format("Segment(from=(%f,%f), to=(%f,%f))",
                getFrom().getX(), getFrom().getY(), getTo().getX(), getTo().getY());
    }
}
