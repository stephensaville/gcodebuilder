package com.gcodebuilder.geometry;

import com.gcodebuilder.canvas.Drawable;
import javafx.scene.canvas.GraphicsContext;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class Drawing implements Drawable {
    @Getter
    private final List<Shape> shapes = new ArrayList<>();

    @Getter @Setter
    private Shape currentShape;

    @Getter @Setter
    private boolean dirty = true;

    public void add(Shape shape) {
        shapes.add(shape);
        dirty = true;
    }

    public boolean remove(Shape shape) {
        if (shapes.remove(shape)) {
            dirty = true;
            return true;
        }
        return false;
    }

    @Override
    public void draw(GraphicsContext ctx, double pixelsPerUnit) {
        for (Drawable shape : shapes) {
            shape.draw(ctx, pixelsPerUnit);
        }
        dirty = false;
    }
}
