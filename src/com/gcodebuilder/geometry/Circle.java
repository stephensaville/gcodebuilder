package com.gcodebuilder.geometry;

import com.gcodebuilder.app.tools.InteractionEvent;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Circle extends Shape<Circle.Handle> {
    private static final Logger log = LogManager.getLogger(Circle.class);

    private double centerX;
    private double centerY;
    private double radius;

    public class Handle {

    }

    public double getMinX() {
        return centerX - radius;
    }

    public double getMaxX() {
        return centerX + radius;
    }

    public double getMinY() {
        return centerY - radius;
    }

    public double getMaxY() {
        return centerY + radius;
    }

    public double getWidth() {
        return radius * 2;
    }

    public double getHeight() {
        return radius * 2;
    }

    public boolean update(double centerX, double centerY, double radius) {
        boolean updated = false;
        if (this.centerX != centerX) {
            this.centerX = centerX;
            updated = true;
        }
        if (this.centerY != centerY) {
            this.centerY = centerY;
            updated = true;
        }
        if (this.radius != radius) {
            this.radius = radius;
            updated = true;
        }
        return updated;
    }

    @Override
    public Handle getHandle(Point2D gridPoint) {
        double distanceToCenter = gridPoint.distance(centerX, centerY);
        log.info(String.format("Measured distance: %f to center: (%f,%f) of circle with radius: %f",
                distanceToCenter, centerX, centerY, radius));
        if (distanceToCenter == radius) {
            return new Handle();
        }
        return null;
    }

    @Override
    public boolean moveHandle(Handle handle, InteractionEvent event) {
        return false;
    }

    @Override
    public void draw(GraphicsContext ctx, double pixelsPerUnit) {
        if (radius > 0) {
            log.info(String.format("Drawing circle: centerX=%f centerY=%f radius=%f", centerX, centerY, radius));
            ctx.setLineWidth(2 / pixelsPerUnit);
            ctx.setStroke(Color.BLACK);
            ctx.strokeOval(getMinX(), getMinY(), getWidth(), getHeight());
        }
    }
}
