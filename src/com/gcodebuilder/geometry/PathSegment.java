package com.gcodebuilder.geometry;

import com.gcodebuilder.generator.toolpath.Toolpath;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

public interface PathSegment {
    Logger log = LogManager.getLogger(PathSegment.class);

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

    UnitVector getFromDirection();

    default UnitVector getToDirection() {
        return getFromDirection();
    }

    default double getFromAngle() {
        return getFromDirection().getAngle();
    }

    default double getToAngle() {
        return getToDirection().getAngle();
    }

    PathSegment move(Point2D offset);

    PathSegment flip();

    default Toolpath.Segment flipToolpathSegment(Toolpath.Segment original) {
        return new Toolpath.Segment(flip(), original.getToolRadius(), !original.isLeftSide(),
                original.getToConnection(), original.getFromConnection());
    }

    Toolpath.Segment computeToolpathSegment(double toolRadius, boolean leftSide);

    default Toolpath.SegmentPair computeToolpathSegments(double toolRadius) {
        return new Toolpath.SegmentPair(
                computeToolpathSegment(toolRadius, true),
                computeToolpathSegment(toolRadius, false));
    }

    @Data
    class SplitSegments {
        private final PathSegment fromSegment;
        private final PathSegment toSegment;
    }

    SplitSegments split(Point2D splitPoint);

    @Data
    class IntersectionPoint {
        private final Point2D point;
        private final boolean onSegments;
    }

    List<IntersectionPoint> intersect(LineSegment other);

    List<IntersectionPoint> intersect(ArcSegment other);

    default List<IntersectionPoint> intersect(PathSegment other) {
        if (other instanceof LineSegment) {
            return intersect((LineSegment)other);
        } else if (other instanceof ArcSegment) {
            return intersect((ArcSegment)other);
        } else {
            return Collections.emptyList();
        }
    }

    Point2D project(Point2D point);

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
    boolean isWindingMatch(Point2D point);

    void draw(GraphicsContext ctx);
}
