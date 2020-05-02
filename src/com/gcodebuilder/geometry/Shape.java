package com.gcodebuilder.geometry;

import com.gcodebuilder.app.tools.InteractionEvent;
import com.gcodebuilder.canvas.Drawable;
import javafx.geometry.Point2D;

public abstract class Shape<H> implements Drawable {
    public abstract H getHandle(Point2D gridPoint);
    public abstract boolean moveHandle(H handle, InteractionEvent event);
}
