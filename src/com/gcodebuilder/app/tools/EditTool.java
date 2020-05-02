package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Shape;

public class EditTool implements Tool {
    @Override
    public Shape down(InteractionEvent event) {
        return event.getShape();
    }

    private void editShape(InteractionEvent event) {
        if (event.getHandle() != null && event.getShape() != null) {
            if (event.getShape().edit(event.getHandle(), event)) {
                event.getDrawing().setDirty(true);
            }
        }
    }

    @Override
    public void drag(InteractionEvent event) {
        editShape(event);
    }

    @Override
    public void up(InteractionEvent event) {
        editShape(event);
    }
}
