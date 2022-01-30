/*
 * Copyright (c) 2021 Stephen Saville
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gcodebuilder.geometry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.gcodebuilder.app.GridSettings;
import com.gcodebuilder.app.tools.InteractionEvent;
import com.gcodebuilder.changelog.Snapshot;
import com.google.common.base.Preconditions;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@JsonTypeName("CIRCLE")
@Getter
@EqualsAndHashCode(callSuper = false)
public class Circle extends SimpleShape<Circle.Handle> {
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
        super(Handle.class);
        Preconditions.checkNotNull(center);
        this.center = center;
        this.radius = radius;
        log.debug("new {}", this);
    }

    @JsonIgnore
    public double getMinX() {
        return center.getX() - radius;
    }

    @JsonIgnore
    public double getCenterX() {
        return center.getX();
    }

    @JsonIgnore
    public double getMaxX() {
        return center.getX() + radius;
    }

    @JsonIgnore
    public double getCenterY() {
        return center.getY();
    }

    @JsonIgnore
    public double getMinY() {
        return center.getY() - radius;
    }

    @JsonIgnore
    public double getMaxY() {
        return center.getY() + radius;
    }

    @JsonIgnore
    public double getWidth() {
        return radius * 2;
    }

    @JsonIgnore
    public double getHeight() {
        return radius * 2;
    }

    @Override
    public Rectangle2D getBoundingBox() {
        return new Rectangle2D(getMinX(), getMinY(), getWidth(), getHeight());
    }

    public boolean updateCenter(Point newCenter) {
        Preconditions.checkNotNull(newCenter);
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
    public boolean move(Point2D delta) {
        return updateCenter(center.add(delta));
    }

    public boolean resize(double scaleFactor, Point center) {
        return updateRadius(radius * scaleFactor);
    }

    @Override
    public Snapshot<Circle> save() {
        return new Snapshot<>() {
            private final Point center = getCenter();
            private final double radius = getRadius();

            @Override
            public Circle restore() {
                update(center, radius);
                return Circle.this;
            }
        };
    }

    @Override
    public Path convertToPath() {
        Path path = new Path();
        path.addPoint(getMinX(), getCenterY());
        path.addPoint(center.asPoint2D(), Point.Type.CW_CENTER);
        path.setClosed(true);
        return path;
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
