package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.changelog.Snapshot;
import com.gcodebuilder.changelog.Change;
import com.gcodebuilder.changelog.UpdateShapeChange;

import java.util.function.Supplier;

public abstract class UpdateShapeTool implements Tool {

    protected abstract void updateShape(InteractionEvent event);
    protected abstract String getDescription();

    @Override
    public Shape<?> down(InteractionEvent event) {
        return event.getShape();
    }

    @Override
    public void drag(InteractionEvent event) {
        updateShape(event);
    }

    @Override
    public void up(InteractionEvent event) {
        updateShape(event);
    }

    @Override
    public Supplier<Change> prepareChange(final Drawing drawing, final Shape<?> shape) {
        if (shape != null) {
            final Snapshot<? extends Shape<?>> before = shape.save();
            return () -> new UpdateShapeChange(getDescription(), drawing, before, shape.save());
        } else {
            return null;
        }
    }
}
