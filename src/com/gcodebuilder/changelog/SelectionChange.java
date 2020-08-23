package com.gcodebuilder.changelog;

import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import lombok.Data;

import java.util.Set;

@Data
public class SelectionChange implements Change {
    private final String description;
    private final Drawing drawing;
    private final Set<Shape<?>> selectionBefore;
    private final Set<Shape<?>> selectionAfter;

    @Override
    public void undo() {
        drawing.setSelectedShapes(selectionBefore);
    }

    @Override
    public void redo() {
        drawing.setSelectedShapes(selectionAfter);
    }
}
