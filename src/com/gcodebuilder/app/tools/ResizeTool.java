package com.gcodebuilder.app.tools;

import com.gcodebuilder.changelog.Change;
import com.gcodebuilder.changelog.Snapshot;
import com.gcodebuilder.changelog.UpdateShapeChange;
import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Math2D;
import com.gcodebuilder.geometry.Point;
import com.gcodebuilder.geometry.Shape;

import java.util.function.Supplier;

public class ResizeTool implements Tool {
    private Snapshot<? extends Shape<?>> original;
    private Point center;
    private double scaleFactor;

    @Override
    public Shape<?> down(InteractionEvent event) {
        Shape<?> shape = event.getShape();
        if (shape != null) {
            original = shape.save();
            center = shape.getCenter();
        } else {
            original = null;
            center = null;
        }
        scaleFactor = 1.0;
        return shape;
    }

    public void resizeShape(InteractionEvent event) {
        if (original != null) {
            double newScaleFactor = Math2D.computeScaleFactor(center.asPoint2D(),
                    event.getStartPoint(), event.getPoint());
            boolean resized;
            if (newScaleFactor == scaleFactor) {
                resized = false;
            } else {
                Shape<?> shape = original.restore();
                shape.resize(newScaleFactor, center);
                scaleFactor = newScaleFactor;
                resized = true;
            }
            event.getDrawing().setDirty(resized);
        }
    }

    @Override
    public void drag(InteractionEvent event) {
        resizeShape(event);
    }

    @Override
    public void up(InteractionEvent event) {
        resizeShape(event);
    }

    @Override
    public Supplier<Change> prepareChange(Drawing drawing, Shape<?> shape) {
        if (shape != null) {
            return () -> {
                if (scaleFactor == 1.0) {
                    return null;
                } else {
                    return new UpdateShapeChange("Resize", drawing, original, shape.save());
                }
            };
        } else {
            return null;
        }
    }
}
