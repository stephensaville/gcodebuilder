package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Shape;

public class MoveTool implements Tool {
    @Override
    public Shape down(InteractionEvent event) {
        return event.getShape();
    }

    private void moveShape(InteractionEvent event) {
        if (event.getHandle() != null && event.getShape() != null) {
            if (event.getShape().move(event.getHandle(), event)) {
                event.getDrawing().setDirty(true);
            }
        }
    }

    @Override
    public void drag(InteractionEvent event) {
        moveShape(event);
    }

    @Override
    public void up(InteractionEvent event) {
        moveShape(event);
    }
}
