package com.gcodebuilder.changelog;

import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import lombok.Data;

@Data
public class UpdateShapeChange implements Change {
    private final String description;
    private final Drawing drawing;
    private final Snapshot<? extends Shape<?>> before;
    private final Snapshot<? extends Shape<?>> after;

    @Override
    public void undo() {
        before.restore();
    }

    @Override
    public void redo() {
        after.restore();
    }
}
