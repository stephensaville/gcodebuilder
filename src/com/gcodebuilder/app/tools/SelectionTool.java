package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Shape;

public class SelectionTool implements Tool {
    @Override
    public boolean isSelectionTool() {
        return true;
    }

    @Override
    public Shape down(InteractionEvent event) {
        if (event.getDrawing().unselectAllShapes() > 0) {
            event.getDrawing().setDirty(true);
        }
        Shape currentShape = event.getShape();
        if (currentShape != null) {
            event.getShape().setSelected(true);
            event.getDrawing().setDirty(true);
        }
        return event.getShape();
    }

    @Override
    public void drag(InteractionEvent event) {

    }

    @Override
    public void up(InteractionEvent event) {
    }
}
