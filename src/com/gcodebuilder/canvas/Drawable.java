package com.gcodebuilder.canvas;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gcodebuilder.app.GridSettings;
import javafx.scene.canvas.GraphicsContext;

public interface Drawable {
    void draw(GraphicsContext ctx, double pixelsPerUnit, GridSettings settings);

    @JsonIgnore
    default boolean isVisible() {
        return true;
    }
}
