package com.gcodebuilder.changelog;

public interface Change {
    String getDescription();
    void undo();
    void redo();
}
