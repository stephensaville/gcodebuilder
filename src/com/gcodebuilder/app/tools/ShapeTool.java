package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Shape;

public abstract class ShapeTool<S extends Shape<?>> implements Tool {
    private final Class<S> shapeClass;

    protected ShapeTool(Class<S> shapeClass) {
        this.shapeClass = shapeClass;
    }

    protected abstract S createShape(InteractionEvent event);

    protected abstract boolean updateShape(InteractionEvent event, S currentShape);

    @Override
    public S down(InteractionEvent event) {
        S newShape = createShape(event);
        event.getDrawing().add(newShape);
        return newShape;
    }

    private S updateCurrentShape(InteractionEvent event) {
        S currentShape = shapeClass.cast(event.getShape());
        if (updateShape(event, currentShape)) {
            event.getDrawing().setDirty(true);
        }
        return currentShape;
    }

    @Override
    public void drag(InteractionEvent event) {
        updateCurrentShape(event);
    }

    @Override
    public void up(InteractionEvent event) {
        S shape = updateCurrentShape(event);
        if (!shape.isVisible()) {
            event.getDrawing().remove(shape);
        }
    }
}
