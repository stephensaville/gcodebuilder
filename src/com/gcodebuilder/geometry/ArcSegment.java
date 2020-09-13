package com.gcodebuilder.geometry;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.ArcType;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Getter
public class ArcSegment implements PathSegment {
    private static final Logger log = LogManager.getLogger(ArcSegment.class);

    private final Point2D from;
    private final Point2D center;
    private final Point2D to;
    private final boolean clockwise;
    private final double radius;
    private final UnitVector direction;
    private final double startAngle;
    private final double extentAngle;

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
        double fromAngle = centerToFrom.getAngle();
        this.radius = centerToFrom.getLength();
        this.startAngle = fromAngle;
        double angleDiff;
        if (Point.isSamePoints(from, to)) {
            angleDiff = clockwise ? -2*Math.PI : 2*Math.PI;
            this.to = from;
        } else {
            LineSegment centerToTo = LineSegment.of(center, to);
            this.to = center.add(centerToTo.getDirection().multiply(radius));
            double toAngle = centerToTo.getAngle();
            angleDiff = Math2D.subtractAngle(toAngle, fromAngle);
        }
        if (clockwise) {
            this.direction = centerToFrom.getDirection().rightNormal();
            this.extentAngle = (angleDiff < 0) ? angleDiff : angleDiff - 2*Math.PI;
        } else {
            this.direction = centerToFrom.getDirection().leftNormal();
            this.extentAngle = (angleDiff > 0) ? angleDiff : angleDiff + 2*Math.PI;
        }
        log.info("Calculated {}: radius={} startAngle={} extentAngle={} using: clockwise={} fromAngle={} toAngle={}",
                this, radius, startAngle, extentAngle, clockwise, fromAngle, angleDiff);
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

    @Override
    public Point2D intersect(PathSegment other, boolean allowOutside) {
        return null;
    }

    @Override
    public Point2D intersect(PathSegment other) {
        return null;
    }

    @Override
    public Point2D project(Point2D point) {
        return null;
    }

    @Override
    public boolean isWindingMatch(Point2D point) {
        return false;
    }

    @Override
    public String toString() {
        return String.format("ArcSegment((%s,%s), (%s,%s), (%s,%s))",
                getFrom().getX(), getFrom().getY(),
                getCenter().getX(), getCenter().getY(),
                getTo().getX(), getTo().getY());
    }
}
