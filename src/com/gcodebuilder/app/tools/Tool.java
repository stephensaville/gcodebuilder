package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.changelog.Change;
import javafx.geometry.Rectangle2D;

import java.util.Formatter;
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

    default void addStatusText(InteractionEvent event, Formatter statusFormatter) {
        Shape<?> shape = event.getShape();
        if (shape != null) {
            Rectangle2D boundingBox = shape.getBoundingBox();
            statusFormatter.format("  w: %.4f  h: %.4f",
                    boundingBox.getWidth(), boundingBox.getHeight());
        }
    }
}
