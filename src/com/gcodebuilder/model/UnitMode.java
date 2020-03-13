package com.gcodebuilder.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum UnitMode implements GCodeWord {
    INCH("G20", 1),
    MM("G21", 25.4);

    private final String gcode;

    @Getter
    private final double unitsPerInch;

    @Override
    public String toGCode() {
        return gcode;
    }
}
