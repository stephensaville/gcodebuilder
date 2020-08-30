package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Shape;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SelectionTool implements Tool {
    private static final Logger log = LogManager.getLogger(SelectionTool.class);

    @Override
    public boolean isSelectionTool() {
        return true;
    }

    @Override
    public Shape<?> down(InteractionEvent event) {
        Shape<?> currentShape = event.getShape();
        if (event.getInputEvent().isControlDown()) {
            // update existing selection when control is down
            if (currentShape != null) {
                event.getShape().setSelected(!event.getShape().isSelected());
                event.getDrawing().setDirty(true);
                log.info("{} selection: {}",
                        event.getShape().isSelected() ? "Added to" : "Removed from",
                        currentShape);
            }
        } else if (event.getInputEvent().isShiftDown()) {
            // add to existing selection when shift is down
            if (currentShape != null) {
                event.getShape().setSelected(true);
                event.getDrawing().setDirty(true);
                log.info("Added to selection: " + currentShape);
            }
        } else {
            // reset selection when control is up
            if (event.getDrawing().setSelectedShapes(currentShape)) {
                event.getDrawing().setDirty(true);
                log.info("Set selection to only: " + currentShape);
            }
        }
        return currentShape;
    }

    @Override
    public void drag(InteractionEvent event) {

    }

    @Override
    public void up(InteractionEvent event) {
    }
}
