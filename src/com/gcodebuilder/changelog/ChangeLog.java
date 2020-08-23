package com.gcodebuilder.changelog;

import java.util.ArrayList;
import java.util.List;

public class ChangeLog {
    private final List<Change> changes = new ArrayList<>();
    private int changeIndex = 0;

    public void doChange(Change change) {
        if (changeIndex < changes.size()) {
            // NOTE: this is a branch point
            changes.subList(changeIndex, changes.size()).clear();
        }
        changes.add(change);
        changeIndex = changes.size();
    }

    public void undoChange() {
        if (changeIndex > 0) {
            changes.get(--changeIndex).undo();
        }
    }

    public boolean isUndoEnabled() {
        return changeIndex > 0;
    }

    public String getUndoDescription() {
        if (changeIndex > 0) {
            return changes.get(changeIndex - 1).getDescription();
        } else {
            return null;
        }
    }

    public void redoChange() {
        if (changeIndex < changes.size()) {
            changes.get(changeIndex++).redo();
        }
    }

    public boolean isRedoEnabled() {
        return changeIndex < changes.size();
    }

    public String getRedoDescription() {
        if (changeIndex < changes.size()) {
            return changes.get(changeIndex).getDescription();
        } else {
            return null;
        }
    }
}
