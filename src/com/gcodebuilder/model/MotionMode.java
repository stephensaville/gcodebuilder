package com.gcodebuilder.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MotionMode implements GCodeWord {
    RAPID_LINEAR("G0"),
    LINEAR("G1"),
    CW_ARC("G2"),
    CCW_ARC("G3");

    private final String gcode;

    @Override
    public String toGCode() {
        return gcode;
    }
}
