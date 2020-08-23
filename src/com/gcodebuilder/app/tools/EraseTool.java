package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.changelog.RemoveShapeChange;
import com.gcodebuilder.changelog.Snapshot;
import com.gcodebuilder.changelog.Change;

import java.util.function.Supplier;

public class EraseTool implements Tool {
    @Override
    public Shape<?> down(InteractionEvent event) {
        if (event.getShape() != null) {
            event.getDrawing().remove(event.getShape());
        }
        return event.getShape();
    }

    @Override
    public void drag(InteractionEvent event) {

    }

    @Override
    public void up(InteractionEvent event) {

    }

    @Override
    public Supplier<Change> prepareChange(Drawing drawing, Shape<?> shape) {
        final Snapshot<? extends Shape<?>> before = shape.save();
        return () -> new RemoveShapeChange("Erase", drawing, before);
    }
}
