package com.gcodebuilder.changelog;

import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import lombok.Data;

@Data
public class AddShapeChange implements Change {
    private final String description;
    private final Drawing drawing;
    private final Snapshot<? extends Shape<?>> after;

    @Override
    public void undo() {
        drawing.remove(after.restore());
    }

    @Override
    public void redo() {
        drawing.add(after.restore());
    }
}
