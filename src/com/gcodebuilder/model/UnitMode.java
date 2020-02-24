package com.gcodebuilder.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum UnitMode implements GCodeWord {
    INCH("G20"),
    MM("G21");

    private final String gcode;

    @Override
    public String toGCode() {
        return gcode;
    }
}
