package com.gcodebuilder.geometry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.gcodebuilder.app.GridSettings;
import com.gcodebuilder.app.tools.InteractionEvent;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@JsonTypeName("CIRCLE")
@Getter
@EqualsAndHashCode
public class Circle extends Shape<Circle.Handle> {
    private static final Logger log = LogManager.getLogger(Circle.class);

    @Getter
    private Point center;

    @Getter
    private double radius;

    @Data
    public class Handle {
        private final Point originalCenter;
        private boolean moved = false;
    }

    @JsonCreator
    public Circle(@JsonProperty("center") Point center,
                  @JsonProperty("radius") double radius) {
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

    public boolean updateCenter(Point newCenter) {
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

    public boolean update(Point newCenter, double newRadius) {
        boolean updated = updateCenter(newCenter);
        updated = updateRadius(newRadius) || updated;
        if (updated) {
            log.debug("update {}", this);
        }
        return updated;
    }

    @Override
    public Handle getHandle(Point2D point, Point2D mousePoint, double handleRadius) {
        double distanceToCenter = center.distance(mousePoint);
        if (Math.abs(distanceToCenter - radius) < handleRadius) {
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
        return updateCenter(handle.getOriginalCenter().add(delta));
    }

    @Override
    public boolean isVisible() {
        return radius > 0;
    }

    @Override
    public void draw(GraphicsContext ctx, double pixelsPerUnit, GridSettings settings) {
        prepareToDraw(ctx, pixelsPerUnit, settings);
        ctx.strokeOval(getMinX(), getMinY(), getWidth(), getHeight());
    }

    @Override
    public String toString() {
        return String.format("Circle(center=(%f, %f), radius=%f)",
                center.getX(), center.getY(), radius);
    }
}
