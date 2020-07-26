package com.gcodebuilder.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ArcDistanceMode implements GCodeWord {
    ABSOLUTE("G90.1"),
    INCREMENTAL("G91.1");

    private final String gcode;

    @Override
    public String toGCode() {
        return gcode;
    }
}
