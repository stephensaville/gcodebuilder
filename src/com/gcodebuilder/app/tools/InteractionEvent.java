package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import javafx.geometry.Point2D;
import javafx.scene.input.InputEvent;
import lombok.Data;

@Data
public class InteractionEvent {
    private final Drawing drawing;
    private final InputEvent inputEvent;
    private final Point2D point;
    private final Point2D startPoint;
    private final Point2D mousePoint;
    private final Point2D mouseStartPoint;
    private final Shape shape;
    private final Object handle;
    private final double handleRadius;
}
