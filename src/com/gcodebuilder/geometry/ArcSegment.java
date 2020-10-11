package com.gcodebuilder.geometry;

import com.gcodebuilder.generator.toolpath.Toolpath;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.ArcType;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Getter
public class ArcSegment implements PathSegment {
    private static final Logger log = LogManager.getLogger(ArcSegment.class);

    private static final Range LEFT_ANGLE_RANGE = Range.inclusiveMinAndMax(Math.PI/2, 3*Math.PI/2);

    private final Point2D from;
    private final Point2D center;
    private final Point2D to;
    private final boolean clockwise;
    private final double radius;
    private final UnitVector fromDirection;
    private final UnitVector toDirection;
    private final double startAngle;
    private final double extentAngle;
    private final List<Range> leftWindingRanges;
    private final List<Range> rightWindingRanges;

    public static ArcSegment of(Point2D from, Point2D center, Point2D to, boolean clockwise) {
        return new ArcSegment(from, center, to, clockwise);
    }

    public static ArcSegment of(Point from, Point center, Point to, boolean clockwise) {
        return of(from.asPoint2D(), center.asPoint2D(), to.asPoint2D(), clockwise);
    }

    public static ArcSegment of(Point from, Point center, Point to) {
        return of(from, center, to, center.isClockwiseCenterPoint());
    }

    protected ArcSegment(Point2D from, Point2D center, Point2D to, boolean clockwise) {
        this.from = from;
        this.center = center;
        this.clockwise = clockwise;
        LineSegment centerToFrom = LineSegment.of(center, from);
        LineSegment centerToTo = LineSegment.of(center, to);
        this.radius = centerToFrom.getLength();
        this.startAngle = centerToFrom.getFromAngle();
        double minY = center.getY() - radius;
        double maxY = center.getY() + radius;
        if (Math2D.samePoints(from, to)) {
            this.extentAngle = clockwise ? -2*Math.PI : 2*Math.PI;
            this.to = from;
            this.leftWindingRanges = this.rightWindingRanges = Collections.singletonList(
                    Range.inclusiveMinExclusiveMax(minY, maxY));
        } else {
            double stopAngle = centerToTo.getFromAngle();
            this.to = to = center.add(centerToTo.getDirection().multiply(radius));
            if (clockwise) {
                this.extentAngle = Math2D.subtractAngle(stopAngle, this.startAngle,
                        -2*Math.PI, 0, false);
                if (LEFT_ANGLE_RANGE.includes(startAngle)) {
                    if (LEFT_ANGLE_RANGE.includes(stopAngle)) {
                        // arc starts and stops in left half of circle
                        if (extentAngle == -Math.PI) {
                            // arc is exactly half of circle
                            if (startAngle < Math.PI) {
                                // arc is right half of circle
                                this.leftWindingRanges = Collections.emptyList();
                                this.rightWindingRanges = Collections.singletonList(
                                        Range.inclusiveMinExclusiveMax(minY, maxY));
                            } else {
                                // arc is left half of circle
                                this.leftWindingRanges = Collections.singletonList(
                                        Range.inclusiveMinExclusiveMax(minY, maxY));
                                this.rightWindingRanges = Collections.emptyList();
                            }
                        } else if (-extentAngle < Math.PI) {
                            // arc contained within left half of circle
                            this.leftWindingRanges = Collections.singletonList(
                                    Range.inclusiveMinExclusiveMax(from.getY(), to.getY()));
                            this.rightWindingRanges = Collections.emptyList();
                        } else {
                            // arc wraps around right half of circle
                            this.leftWindingRanges = Arrays.asList(
                                    Range.inclusiveMinExclusiveMax(minY, to.getY()),
                                    Range.inclusiveMinExclusiveMax(from.getY(), maxY));
                            this.rightWindingRanges = Collections.singletonList(
                                    Range.inclusiveMinExclusiveMax(minY, maxY));
                        }
                    } else {
                        // arc starts in left and stops in right half of circle
                        this.leftWindingRanges = Collections.singletonList(
                                Range.inclusiveMinExclusiveMax(from.getY(), maxY));
                        this.rightWindingRanges = Collections.singletonList(
                                Range.inclusiveMinExclusiveMax(to.getY(), maxY));
                    }
                } else {
                    if (LEFT_ANGLE_RANGE.includes(stopAngle)) {
                        // arc starts in right and stops in left half of circle
                        this.leftWindingRanges = Collections.singletonList(
                                Range.inclusiveMinExclusiveMax(minY, to.getY()));
                        this.rightWindingRanges = Collections.singletonList(
                                Range.inclusiveMinExclusiveMax(minY, from.getY()));
                    } else {
                        // arc starts and stops in right half of circle
                        if (-extentAngle <= Math.PI) {
                            // arc contained within right half of circle
                            this.leftWindingRanges = Collections.emptyList();
                            this.rightWindingRanges = Collections.singletonList(
                                    Range.inclusiveMinExclusiveMax(to.getY(), from.getY()));
                        } else {
                            // arc wraps around left half of circle
                            this.leftWindingRanges = Collections.singletonList(
                                    Range.inclusiveMinExclusiveMax(minY, maxY));
                            this.rightWindingRanges = Arrays.asList(
                                    Range.inclusiveMinExclusiveMax(minY, from.getY()),
                                    Range.inclusiveMinExclusiveMax(to.getY(), maxY));
                        }
                    }
                }
            } else {
                this.extentAngle = Math2D.subtractAngle(stopAngle, this.startAngle,
                        0, 2*Math.PI, true);
                if (LEFT_ANGLE_RANGE.includes(startAngle)) {
                    if (LEFT_ANGLE_RANGE.includes(stopAngle)) {
                        // arc starts and stops in left half of circle
                        if (extentAngle == Math.PI) {
                            // arc is exactly half of circle
                            if (startAngle < Math.PI) {
                                // arc is left half of circle
                                this.leftWindingRanges = Collections.singletonList(
                                        Range.inclusiveMinExclusiveMax(minY, maxY));
                                this.rightWindingRanges = Collections.emptyList();
                            } else {
                                // arc is right half of circle
                                this.leftWindingRanges = Collections.emptyList();
                                this.rightWindingRanges = Collections.singletonList(
                                        Range.inclusiveMinExclusiveMax(minY, maxY));
                            }
                        } else if (extentAngle < Math.PI) {
                            // arc contained within left half of circle
                            this.leftWindingRanges = Collections.singletonList(
                                    Range.inclusiveMinExclusiveMax(to.getY(), from.getY()));
                            this.rightWindingRanges = Collections.emptyList();
                        } else {
                            // arc wraps around right half of circle
                            this.leftWindingRanges = Arrays.asList(
                                    Range.inclusiveMinExclusiveMax(minY, from.getY()),
                                    Range.inclusiveMinExclusiveMax(to.getY(), maxY));
                            this.rightWindingRanges = Collections.singletonList(
                                    Range.inclusiveMinExclusiveMax(minY, maxY));
                        }
                    } else {
                        // arc starts in left and stops in right half of circle
                        this.leftWindingRanges = Collections.singletonList(
                                Range.inclusiveMinExclusiveMax(minY, from.getY()));
                        this.rightWindingRanges = Collections.singletonList(
                                Range.inclusiveMinExclusiveMax(minY, to.getY()));
                    }
                } else {
                    if (LEFT_ANGLE_RANGE.includes(stopAngle)) {
                        // arc starts in right and stops in left half of circle
                        this.leftWindingRanges = Collections.singletonList(
                                Range.inclusiveMinExclusiveMax(to.getY(), maxY));
                        this.rightWindingRanges = Collections.singletonList(
                                Range.inclusiveMinExclusiveMax(from.getY(), maxY));
                    } else {
                        // arc starts and stops in right half of circle
                        if (extentAngle <= Math.PI) {
                            // arc contained within right half of circle
                            this.leftWindingRanges = Collections.emptyList();
                            this.rightWindingRanges = Collections.singletonList(
                                    Range.inclusiveMinExclusiveMax(from.getY(), to.getY()));
                        } else {
                            // arc wraps around left half of circle
                            this.leftWindingRanges = Collections.singletonList(
                                    Range.inclusiveMinExclusiveMax(minY, maxY));
                            this.rightWindingRanges = Arrays.asList(
                                    Range.inclusiveMinExclusiveMax(minY, to.getY()),
                                    Range.inclusiveMinExclusiveMax(from.getY(), maxY));
                        }
                    }
                }
            }
        }
        this.fromDirection = clockwise
                ? centerToFrom.getDirection().rightNormal()
                : centerToFrom.getDirection().leftNormal();
        this.toDirection = clockwise
                ? centerToTo.getToDirection().rightNormal()
                : centerToTo.getToDirection().leftNormal();
    }

    public double getRadiusSquared() {
        return radius * radius;
    }

    public void draw(GraphicsContext ctx) {
        ctx.strokeArc(center.getX() - radius, center.getY() - radius, radius*2, radius*2,
                360 - Math2D.convertToDegrees(startAngle),
                -Math2D.convertToDegrees(extentAngle),
                ArcType.OPEN);
    }

    @Override
    public ArcSegment move(Point2D offset) {
        return new ArcSegment(from.add(offset), center.add(offset), to.add(offset), clockwise);
    }

    @Override
    public ArcSegment flip() {
        return new ArcSegment(to, center, from, !clockwise);
    }

    public boolean isToolpathSegmentOutside(boolean leftSide) {
        return clockwise == leftSide;
    }

    @Override
    public UnitVector getDirectionAtPoint(Point2D point) {
        UnitVector centerToPoint = UnitVector.from(center, point);
        return clockwise ? centerToPoint.rightNormal() : centerToPoint.leftNormal();
    }

    @Override
    public Toolpath.Segment computeToolpathSegment(double toolRadius, boolean leftSide) {
        UnitVector awayFromFrom, awayFromTo;
        if (isToolpathSegmentOutside(leftSide)) {
            // computing outside arc segment
            awayFromFrom = UnitVector.from(center, from);
            awayFromTo = UnitVector.from(center, to);
        } else {
            // computing inside arc segment
            awayFromFrom = UnitVector.from(from, center);
            awayFromTo = UnitVector.from(to, center);
        }
        Point2D toolpathFrom = from.add(awayFromFrom.multiply(toolRadius));
        Point2D toolpathTo = to.add(awayFromTo.multiply(toolRadius));
        ArcSegment toolpathSegment = new ArcSegment(toolpathFrom, center, toolpathTo, clockwise);
        return new Toolpath.Segment(toolpathSegment, toolRadius, leftSide,
                new Toolpath.Connection(getFrom()), new Toolpath.Connection(getTo()));
    }

    @Override
    public SplitSegments split(Point2D splitPoint) {
        ArcSegment fromSegment = new ArcSegment(from, center, splitPoint, clockwise);
        ArcSegment toSegment = new ArcSegment(splitPoint, center, to, clockwise);
        return new SplitSegments(fromSegment, toSegment);
    }

    @Override
    public Comparator<Point2D> splitPointComparator() {
        return (left, right) -> {
            double angleToLeft = UnitVector.from(center, left).getAngle();
            double angleToRight = UnitVector.from(center, right).getAngle();
            if (clockwise) {
                double angleToLeftDiff = Math2D.subtractAngle(angleToLeft, startAngle,
                        -2*Math.PI, 0, false);
                double angleToRightDiff = Math2D.subtractAngle(angleToRight, startAngle,
                        -2*Math.PI, 0, false);
                return Double.compare(-angleToLeftDiff, -angleToRightDiff);
            } else {
                double angleToLeftDiff = Math2D.subtractAngle(angleToLeft, startAngle,
                        0, 2*Math.PI, true);
                double angleToRightDiff = Math2D.subtractAngle(angleToRight, startAngle,
                        0, 2*Math.PI, true);
                return Double.compare(angleToLeftDiff, angleToRightDiff);
            }
        };
    }

    private boolean isAngleInArcSegment(double angle) {
        if (clockwise) {
            double angleToPoint = Math2D.subtractAngle(angle, startAngle,
                    -2*Math.PI, 0, false);
            return angleToPoint > extentAngle && angleToPoint < 0;
        } else {
            double angleToPoint = Math2D.subtractAngle(angle, startAngle,
                    0, 2*Math.PI, true);
            return angleToPoint > 0 && angleToPoint < extentAngle;
        }
    }

    private boolean isPointOnArcSegment(Point2D point) {
        return isAngleInArcSegment(LineSegment.of(center, point).getFromAngle());
    }

    @Override
    public List<IntersectionPoint> intersect(LineSegment other) {
        Point2D fromToCenter = center.subtract(other.getFrom());
        double fromToProjectionDistance = fromToCenter.dotProduct(other.getDirection());
        Point2D projectionPoint = other.getFrom().add(other.getDirection().multiply(fromToProjectionDistance));

        LineSegment centerToProjection = LineSegment.of(getCenter(), projectionPoint);
        if (centerToProjection.getLength() > radius - Math2D.SAME_POINT_DISTANCE) {
            // segment too far away to intersection arc
            return Collections.emptyList();
        }

        // distance along segment between projection point and intersection points
        double projectionToIntersectionDistance = Math.sqrt(
                getRadiusSquared() - centerToProjection.getLengthSquared());

        // vector from projection point to intersection point in segment direction
        Point2D projectionToIntersectionVector = other.getDirection().multiply(projectionToIntersectionDistance);

        // check if from side intersection lies within segments
        Point2D fromSideIntersectionPoint = projectionPoint.subtract(projectionToIntersectionVector);
        double fromSideIntersectionDistance = fromToProjectionDistance - projectionToIntersectionDistance;
        boolean fromSideOnSegments = fromSideIntersectionDistance > 0
                && fromSideIntersectionDistance < other.getLength()
                && isPointOnArcSegment(fromSideIntersectionPoint);

        // check if to side intersection lies within segments
        Point2D toSideIntersectionPoint = projectionPoint.add(projectionToIntersectionVector);
        double toSideIntersectionDistance = fromToProjectionDistance + projectionToIntersectionDistance;
        boolean toSideOnSegments = toSideIntersectionDistance > 0
                && toSideIntersectionDistance < other.getLength()
                && isPointOnArcSegment(toSideIntersectionPoint);

        return Arrays.asList(
                new IntersectionPoint(fromSideIntersectionPoint, fromSideOnSegments),
                new IntersectionPoint(toSideIntersectionPoint, toSideOnSegments));
    }

    @Override
    public List<IntersectionPoint> intersect(ArcSegment other) {
        LineSegment centerToCenter = LineSegment.of(center, other.getCenter());
        if (centerToCenter.getLength() >= radius + other.getRadius() - Math2D.SAME_POINT_DISTANCE) {
            // arc too far away to intersect
            return Collections.emptyList();
        }

        // distance from center to line segment between intersection points
        double centerToProjection =
                (getRadiusSquared() - other.getRadiusSquared() + centerToCenter.getLengthSquared())
                / (2 * centerToCenter.getLength());

        // intersection of line between centers and line between intersection points
        Point2D projectionPoint = center.add(centerToCenter.getDirection().multiply(centerToProjection));

        // distance between projection point and intersection points
        double projectionToIntersectionDistance = Math.sqrt(
                getRadiusSquared() - centerToProjection * centerToProjection);

        // vector from projection point to right intersection point
        Point2D projectionToIntersectionVector = centerToCenter.getDirection()
                .rightNormal().multiply(projectionToIntersectionDistance);

        // check if left intersection point lies within segments
        Point2D leftIntersectionPoint = projectionPoint.subtract(projectionToIntersectionVector);
        boolean leftSideOnSegments = other.isPointOnArcSegment(leftIntersectionPoint)
                && isPointOnArcSegment(leftIntersectionPoint);

        // check if right intersection point lies within segments
        Point2D rightIntersectionPoint = projectionPoint.add(projectionToIntersectionVector);
        boolean rightSideOnSegments = other.isPointOnArcSegment(rightIntersectionPoint)
                && isPointOnArcSegment(rightIntersectionPoint);

        return Arrays.asList(
                new IntersectionPoint(leftIntersectionPoint, leftSideOnSegments),
                new IntersectionPoint(rightIntersectionPoint, rightSideOnSegments));
    }

    @Override
    public Point2D project(Point2D point) {
        LineSegment centerToPoint = LineSegment.of(center, point);
        if (isAngleInArcSegment(centerToPoint.getFromAngle())) {
            return center.add(centerToPoint.getDirection().multiply(radius));
        } else {
            return null;
        }
    }

    @Override
    public boolean isWindingMatch(Point2D point) {
        double yOffset = point.getY() - center.getY();
        if (yOffset < -radius || yOffset >= radius) {
            return false;
        }
        double xOffset = Math.sqrt(getRadiusSquared() - yOffset * yOffset);
        boolean windingMatch = false;

        Point2D leftPoint = new Point2D(center.getX() - xOffset, point.getY());
        if (point.getX() < leftPoint.getX()) {
            for (Range range : leftWindingRanges) {
                if (range.includes(point.getY())) {
                    windingMatch = !windingMatch;
                }
            }
        }

        Point2D rightPoint = new Point2D(center.getX() + xOffset, point.getY());
        if (point.getX() < rightPoint.getX()) {
            for (Range range : rightWindingRanges) {
                if (range.includes(point.getY())) {
                    windingMatch = !windingMatch;
                }
            }
        }

        return windingMatch;
    }

    @Override
    public String toString() {
        return String.format("ArcSegment((%s,%s), (%s,%s), (%s,%s), %s)",
                getFrom().getX(), getFrom().getY(),
                getCenter().getX(), getCenter().getY(),
                getTo().getX(), getTo().getY(),
                clockwise ? "CW" : "CCW");
    }
}
