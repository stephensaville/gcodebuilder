package com.gcodebuilder.canvas;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javafx.scene.canvas.GraphicsContext;

public interface Drawable {
    void draw(GraphicsContext ctx, double pixelsPerUnit);

    @JsonIgnore
    default boolean isVisible() {
        return true;
    }
}
