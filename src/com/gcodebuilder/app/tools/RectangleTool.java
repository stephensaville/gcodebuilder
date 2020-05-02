package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Rectangle;
import com.gcodebuilder.geometry.Shape;
import javafx.geometry.Rectangle2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RectangleTool implements Tool {
    private static final Logger log = LogManager.getLogger(RectangleTool.class);

    private static Rectangle2D eventToRect(InteractionEvent event) {
        double minX = Math.min(event.getPoint().getX(), event.getStartPoint().getX());
        double minY = Math.min(event.getPoint().getY(), event.getStartPoint().getY());
        double width = Math.abs(event.getPoint().getX() - event.getStartPoint().getX());
        double height = Math.abs(event.getPoint().getY() - event.getStartPoint().getY());
        return new Rectangle2D(minX, minY, width, height);
    }

    @Override
    public Shape down(InteractionEvent event) {
        Rectangle newShape = new Rectangle(eventToRect(event));
        event.getDrawing().add(newShape);
        return newShape;
    }

    private Rectangle updateRect(InteractionEvent event) {
        Rectangle currentShape = (Rectangle)event.getShape();
        if (currentShape.update(eventToRect(event))) {
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
        Rectangle2D rect = shape.getRect();
        if (rect.getWidth() == 0 && rect.getHeight() == 0) {
            event.getDrawing().remove(shape);
        }
    }
}
