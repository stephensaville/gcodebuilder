package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Rectangle;
import com.gcodebuilder.geometry.Shape;
import javafx.geometry.Rectangle2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RectangleTool implements Tool {
    private static final Logger log = LogManager.getLogger(RectangleTool.class);

    private static final RectangleTool INSTANCE = new RectangleTool();
    public static RectangleTool instance() {
        return INSTANCE;
    }

    private static Rectangle2D eventToRect(InteractionEvent event) {
        double minX = Math.min(event.getX(), event.getStartX());
        double minY = Math.min(event.getY(), event.getStartY());
        double width = Math.abs(event.getX() - event.getStartX());
        double height = Math.abs(event.getY() - event.getStartY());
        return new Rectangle2D(minX, minY, width, height);
    }

    @Override
    public Shape down(InteractionEvent event) {
        Rectangle currentShape = (Rectangle)event.getShape();
        if (currentShape == null) {
            currentShape = new Rectangle(Rectangle2D.EMPTY);
        }
        return currentShape;
    }

    private void updateRect(InteractionEvent event) {
        Rectangle currentShape = (Rectangle)event.getShape();
        boolean changed = currentShape.moveHandle(event.getHandle(), event);
        event.getDrawing().setDirty(changed);
    }

    @Override
    public void drag(InteractionEvent event) {
        updateRect(event);
    }

    @Override
    public void up(InteractionEvent event) {
        updateRect(event);
    }
}
