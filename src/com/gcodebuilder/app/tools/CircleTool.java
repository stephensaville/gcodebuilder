package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Circle;
import com.gcodebuilder.geometry.Shape;
import javafx.geometry.Point2D;

public class CircleTool implements Tool {
    @FunctionalInterface
    private interface CircleFunction<T> {
        T apply(Point2D center, double radius);
    }

    private static <T> T eventToCircle(InteractionEvent event, CircleFunction<T> function) {
        Point2D center = event.getStartPoint();
        double radius = center.distance(event.getPoint());
        return function.apply(center, radius);
    }

    @Override
    public Shape down(InteractionEvent event) {
        Circle newShape = eventToCircle(event, Circle::new);
        event.getDrawing().add(newShape);
        return newShape;
    }

    private Circle updateCircle(InteractionEvent event) {
        Circle shape = (Circle)event.getShape();
        if (eventToCircle(event, shape::update)) {
            event.getDrawing().setDirty(true);
        }
        return shape;
    }

    @Override
    public void drag(InteractionEvent event) {
        updateCircle(event);
    }

    @Override
    public void up(InteractionEvent event) {
        Circle shape = updateCircle(event);
        if (!shape.isVisible()) {
            event.getDrawing().remove(shape);
        }
    }
}
