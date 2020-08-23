package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.changelog.AddShapeChange;
import com.gcodebuilder.changelog.Change;

import java.util.function.Supplier;

public abstract class AddShapeTool<S extends Shape<?>> implements Tool {
    private final Class<S> shapeClass;

    protected AddShapeTool(Class<S> shapeClass) {
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

    @Override
    public Supplier<Change> prepareChange(final Drawing drawing, final Shape<?> shape) {
        return () -> {
            if (shape.isVisible()) {
                return new AddShapeChange(shapeClass.getSimpleName(), drawing, shape.save());
            } else {
                return null;
            }
        };
    }
}
