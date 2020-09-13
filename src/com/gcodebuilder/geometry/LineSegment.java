package com.gcodebuilder.geometry;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Getter
public class LineSegment extends Line implements PathSegment {
    private static final Logger log = LogManager.getLogger(LineSegment.class);

    private final Point2D to;
    private final Point2D vector;
    private final double length;

    public static LineSegment of(Point2D from, Point2D to) {
        return new LineSegment(from, to, to.subtract(from));
    }

    public static PathSegment of(Point from, Point to) {
        return of(from.asPoint2D(), to.asPoint2D());
    }

    public static PathSegment of(double fromX, double fromY, double toX, double toY) {
        return of(new Point2D(fromX, fromY), new Point2D(toX, toY));
    }

    protected LineSegment(Point2D from, Point2D to, Point2D vector) {
        super(from, UnitVector.from(vector));
        this.to = to;
        this.vector = vector;
        this.length = vector.magnitude();
    }

    protected LineSegment(Point2D from, Point2D to, Point2D vector, UnitVector direction, double length) {
        super(from, direction);
        this.to = to;
        this.vector = vector;
        this.length = length;
    }

    @Override
    public LineSegment move(Point2D offset) {
        return new LineSegment(getFrom().add(offset), getTo().add(offset), getVector(), getDirection(), getLength());
    }

    @Override
    public LineSegment flip() {
        return new LineSegment(getTo(), getFrom(), getVector().multiply(-1), getDirection().invert(), getLength());
    }

    public Point2D intersect(LineSegment other, boolean allowOutside) {
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

    @Override
    public Point2D intersect(PathSegment other, boolean allowOutside) {
        if (other instanceof LineSegment) {
            return intersect((LineSegment)other, allowOutside);
        } else {
            return null;
        }
    }

    @Override
    public Point2D intersect(PathSegment other) {
        return intersect(other, false);
    }

    @Override
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
    @Override
    public boolean isWindingMatch(Point2D point) {
        return (point.getY() < getFrom().getY() != point.getY() < getTo().getY()) &&
                point.getX() < calculateX(point.getY());
    }

    @Override
    public void draw(GraphicsContext ctx) {
        ctx.strokeLine(getFrom().getX(), getFrom().getY(), getTo().getX(), getTo().getY());
    }

    @Override
    public String toString() {
        return String.format("Segment((%s,%s), (%s,%s))",
                getFrom().getX(), getFrom().getY(), getTo().getX(), getTo().getY());
    }
}
