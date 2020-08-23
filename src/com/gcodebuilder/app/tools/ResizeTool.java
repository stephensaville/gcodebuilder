package com.gcodebuilder.app.tools;

public class ResizeTool extends UpdateShapeTool {
    @Override
    protected String getDescription() {
        return "Resize";
    }

    @Override
    protected void updateShape(InteractionEvent event) {
        if (event.getHandle() != null && event.getShape() != null) {
            if (event.getShape().castAndResize(event.getHandle(), event)) {
                event.getDrawing().setDirty(true);
            }
        }
    }
}
