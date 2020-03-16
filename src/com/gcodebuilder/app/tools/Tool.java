package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Shape;

public interface Tool {
    Shape down(InteractionEvent event);
    void drag(InteractionEvent event);
    void up(InteractionEvent event);
}
