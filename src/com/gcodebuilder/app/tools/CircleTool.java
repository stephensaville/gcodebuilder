package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Circle;
import com.gcodebuilder.geometry.Point;

public class CircleTool extends ShapeTool<Circle> {
    @FunctionalInterface
    private interface CircleFunction<T> {
        T apply(Point center, double radius);
    }

    private static <T> T eventToCircle(InteractionEvent event, CircleFunction<T> function) {
        Point center = new Point(event.getStartPoint());
        double radius = center.distance(event.getPoint());
        return function.apply(center, radius);
    }

    public CircleTool() {
        super(Circle.class);
    }

    @Override
    protected Circle createShape(InteractionEvent event) {
        return eventToCircle(event, Circle::new);
    }

    @Override
    protected boolean updateShape(InteractionEvent event, Circle currentShape) {
        return eventToCircle(event, currentShape::update);
    }
}
