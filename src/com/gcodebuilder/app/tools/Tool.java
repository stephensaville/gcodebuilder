package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.changelog.Change;

import java.util.function.Supplier;

public interface Tool {
    Shape<?> down(InteractionEvent event);
    void drag(InteractionEvent event);
    void up(InteractionEvent event);

    default boolean isSelectionTool() {
        return false;
    }

    default Supplier<Change> prepareChange(Drawing drawing, Shape<?> shape) {
        return null;
    }
}
