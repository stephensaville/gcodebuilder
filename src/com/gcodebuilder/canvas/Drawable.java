package com.gcodebuilder.canvas;

import javafx.scene.canvas.GraphicsContext;

public interface Drawable {
    void draw(GraphicsContext ctx, double pixelsPerUnit);

    default boolean isVisible() {
        return true;
    }
}
