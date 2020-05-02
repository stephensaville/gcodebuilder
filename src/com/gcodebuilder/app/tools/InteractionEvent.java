package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import javafx.scene.input.InputEvent;
import lombok.Data;

@Data
public class InteractionEvent {
    private final Drawing drawing;
    private final InputEvent inputEvent;
    private final double x;
    private final double y;
    private final double startX;
    private final double startY;
    private final Shape shape;
    private final Object handle;
}
