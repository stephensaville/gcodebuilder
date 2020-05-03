package com.gcodebuilder.geometry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.geometry.Point2D;

public class Point extends Point2D {
    @JsonCreator
    public Point(@JsonProperty("x") double x,
                 @JsonProperty("y") double y) {
        super(x, y);
    }
}
