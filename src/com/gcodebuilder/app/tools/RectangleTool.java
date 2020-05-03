package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Rectangle;
import com.gcodebuilder.geometry.Shape;

public class RectangleTool implements Tool {
    @FunctionalInterface
    private interface RectangleFunction<T> {
        T apply(double minX, double minY, double width, double height);
    }

    private static <T> T eventToRect(InteractionEvent event, RectangleFunction<T> function) {
        double minX = Math.min(event.getPoint().getX(), event.getStartPoint().getX());
        double minY = Math.min(event.getPoint().getY(), event.getStartPoint().getY());
        double width = Math.abs(event.getPoint().getX() - event.getStartPoint().getX());
        double height = Math.abs(event.getPoint().getY() - event.getStartPoint().getY());
        return function.apply(minX, minY, width, height);
    }

    @Override
    public Shape down(InteractionEvent event) {
        Rectangle newShape = eventToRect(event, Rectangle::new);
        event.getDrawing().add(newShape);
        return newShape;
    }

    private Rectangle updateRect(InteractionEvent event) {
        Rectangle currentShape = (Rectangle)event.getShape();
        if (eventToRect(event, currentShape::update)) {
            event.getDrawing().setDirty(true);
        }
        return currentShape;
    }

    @Override
    public void drag(InteractionEvent event) {
        updateRect(event);
    }

    @Override
    public void up(InteractionEvent event) {
        Rectangle shape = updateRect(event);
        if (!shape.isVisible()) {
            event.getDrawing().remove(shape);
        }
    }
}
