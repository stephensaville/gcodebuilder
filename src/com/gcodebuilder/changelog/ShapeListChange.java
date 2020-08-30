package com.gcodebuilder.changelog;

import com.gcodebuilder.geometry.Shape;
import lombok.Data;

import java.util.List;

@Data
public class ShapeListChange implements Change {
    private final String description;
    private final Snapshot<List<Shape<?>>> before;
    private final Snapshot<List<Shape<?>>> after;

    @Override
    public void undo() {
        before.restore();
    }

    @Override
    public void redo() {
        after.restore();
    }
}
