package com.gcodebuilder.app.tools;

public class EditTool extends UpdateShapeTool {
    @Override
    protected String getDescription() {
        return "Edit";
    }

    @Override
    protected void updateShape(InteractionEvent event) {
        if (event.getHandle() != null && event.getShape() != null) {
            if (event.getShape().castAndEdit(event.getHandle(), event)) {
                event.getDrawing().setDirty(true);
            }
        }
    }
}
