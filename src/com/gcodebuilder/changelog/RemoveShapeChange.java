package com.gcodebuilder.changelog;

import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import lombok.Data;

@Data
public class RemoveShapeChange implements Change {
    private final String description;
    private final Drawing drawing;
    private final Snapshot<? extends Shape<?>> before;

    @Override
    public void undo() {
        drawing.add(before.restore());
    }

    @Override
    public void redo() {
        drawing.remove(before.restore());
    }
}
