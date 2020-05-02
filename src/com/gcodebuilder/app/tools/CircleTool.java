package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Circle;
import com.gcodebuilder.geometry.Shape;

public class CircleTool implements Tool {
    private interface CircleFunction<T> {
        T apply(double centerX, double centerY, double radius);
    }

    public <T> T eventToCircle(InteractionEvent event, CircleFunction<T> function) {
        double signedWidth = event.getX() - event.getStartX();
        double signedHeight = event.getY() - event.getStartY();
        double radius = Math.max(Math.abs(signedWidth), Math.abs(signedHeight)) / 2;
        double centerX = event.getStartX() + Math.copySign(radius, signedWidth);
        double centerY = event.getStartY() + Math.copySign(radius, signedHeight);
        return function.apply(centerX, centerY, radius);
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
        if (shape.getRadius() == 0) {
            event.getDrawing().remove(shape);
        }
    }
}
