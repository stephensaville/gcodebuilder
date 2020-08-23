package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Shape;

public class EraseTool implements Tool {
    @Override
    public Shape<?> down(InteractionEvent event) {
        if (event.getShape() != null) {
            event.getDrawing().remove(event.getShape());
        }
        return null;
    }

    @Override
    public void drag(InteractionEvent event) {

    }

    @Override
    public void up(InteractionEvent event) {

    }
}
