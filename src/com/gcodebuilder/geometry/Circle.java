package com.gcodebuilder.geometry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gcodebuilder.app.tools.InteractionEvent;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Getter
@Setter
@EqualsAndHashCode
public class Circle extends Shape<Circle.Handle> {
    private static final Logger log = LogManager.getLogger(Circle.class);

    private static final int LINE_WIDTH = 2;
    private static final int HANDLE_RADIUS = 5;

    private Point2D center;
    private double radius;

    @Data
    public class Handle {
        private final Point2D originalCenter;
        private boolean moved = false;
    }

    @JsonCreator
    public Circle(@JsonProperty("center") Point center,
                  @JsonProperty("radius") double radius) {
        this.center = center;
        this.radius = radius;
        log.debug("new {}", this);
    }

    public Circle(Point2D center, double radius) {
        this.center = center;
        this.radius = radius;
        log.debug("new {}", this);
    }

    @JsonIgnore
    public double getMinX() {
        return center.getX() - radius;
    }

    @JsonIgnore
    public double getMinY() {
        return center.getY() - radius;
    }

    @JsonIgnore
    public double getWidth() {
        return radius * 2;
    }

    @JsonIgnore
    public double getHeight() {
        return radius * 2;
    }

    public boolean updateCenter(Point2D newCenter) {
        if (!center.equals(newCenter)) {
            center = newCenter;
            return true;
        } else {
            return false;
        }
    }

    public boolean updateRadius(double newRadius) {
        if (radius != newRadius) {
            radius = newRadius;
            return true;
        } else {
            return false;
        }
    }

    public boolean update(Point2D newCenter, double newRadius) {
        boolean updated = updateCenter(newCenter);
        updated = updateRadius(newRadius) || updated;
        if (updated) {
            log.debug("update {}", this);
        }
        return updated;
    }

    @Override
    public Handle getHandle(Point2D point, Point2D mousePoint, double pixelsPerUnit) {
        double distanceToCenter = center.distance(mousePoint);
        if (Math.abs(distanceToCenter - radius)*pixelsPerUnit < HANDLE_RADIUS) {
            return new Handle(center);
        }
        return null;
    }

    @Override
    public boolean edit(Handle handle, InteractionEvent event) {
        boolean moved = handle.isMoved();
        if (!moved && !event.getPoint().equals(event.getStartPoint())) {
            handle.setMoved(true);
            moved = true;
        }
        if (moved) {
            return update(center, center.distance(event.getPoint()));
        } else {
            return false;
        }
    }

    @Override
    public boolean move(Handle handle, InteractionEvent event) {
        Point2D delta = event.getPoint().subtract(event.getStartPoint());
        Point2D movedCenter = handle.getOriginalCenter().add(delta);
        return updateCenter(movedCenter);
    }

    @Override
    public void draw(GraphicsContext ctx, double pixelsPerUnit) {
        if (radius > 0) {
            log.debug("draw {}", this);
            ctx.setLineWidth(LINE_WIDTH / pixelsPerUnit);
            ctx.setStroke(Color.BLACK);
            ctx.strokeOval(getMinX(), getMinY(), getWidth(), getHeight());
        }
    }

    @Override
    public String toString() {
        return String.format("Circle(center=(%f, %f), radius=%f)",
                center.getX(), center.getY(), radius);
    }
}
