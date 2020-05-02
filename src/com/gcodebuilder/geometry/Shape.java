package com.gcodebuilder.geometry;

import com.gcodebuilder.app.tools.InteractionEvent;
import com.gcodebuilder.canvas.Drawable;
import javafx.geometry.Point2D;

public abstract class Shape<H> implements Drawable {
    public abstract H getHandle(Point2D point, Point2D mousePoint, double pixelsPerUnit);
    public abstract boolean edit(H handle, InteractionEvent event);
    public abstract boolean move(H handle, InteractionEvent event);
}
