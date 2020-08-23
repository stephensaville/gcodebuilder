package com.gcodebuilder.app.tools;

public class MoveTool extends UpdateShapeTool {
    @Override
    protected String getDescription() {
        return "Move";
    }

    @Override
    protected void updateShape(InteractionEvent event) {
        if (event.getHandle() != null && event.getShape() != null) {
            if (event.getShape().castAndMove(event.getHandle(), event)) {
                event.getDrawing().setDirty(true);
            }
        }
    }
}
