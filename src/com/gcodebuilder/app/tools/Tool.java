package com.gcodebuilder.app.tools;

public interface Tool {
    void down(InteractionEvent event);
    void drag(InteractionEvent event);
    void up(InteractionEvent event);
}
