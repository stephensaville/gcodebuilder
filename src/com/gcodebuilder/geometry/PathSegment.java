package com.gcodebuilder.geometry;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;

import java.util.List;

public interface PathSegment {
    /**
     * Checks if a point is inside the path defined by a list of segments. This function assumes the path is closed,
     * but does not check if the path is closed, so this function should only be used with paths known to be closed.
     *
     * @param path path defined by a list of segments
     * @param point a point
     * @return true if point is inside the path, false if point is outside the path
     */
    static boolean isPointInsidePath(List<PathSegment> path, Point2D point) {
        boolean pointInPath = false;
        for (PathSegment segment : path) {
            if (segment.isWindingMatch(point)) {
                pointInPath = !pointInPath;
            }
        }
        return pointInPath;
    }

    Point2D getFrom();

    Point2D getTo();

    UnitVector getDirection();

    default double getAngle() {
        return getDirection().getAngle();
    }

    PathSegment move(Point2D offset);

    PathSegment flip();

    Point2D intersect(PathSegment other, boolean allowOutside);

    Point2D intersect(PathSegment other);

    Point2D project(Point2D point);

    boolean isWindingMatch(Point2D point);

    void draw(GraphicsContext ctx);
}
