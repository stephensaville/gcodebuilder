package com.gcodebuilder.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum FeedRateMode implements GCodeWord {
    INVERSE_TIME("G93"),
    UNITS_PER_MIN("G94"),
    UNITS_PER_REV("G95");

    private final String gcode;

    @Override
    public String toGCode() {
        return gcode;
    }
}
