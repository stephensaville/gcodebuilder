package com.gcodebuilder.geometry;

import com.gcodebuilder.app.tools.InteractionEvent;
import com.gcodebuilder.canvas.Drawable;
import javafx.geometry.Point2D;

public abstract class Shape implements Drawable {
    public abstract Enum<?> getHandle(Point2D gridPoint);
    public abstract boolean moveHandle(Enum<?> handle, InteractionEvent event);
}
