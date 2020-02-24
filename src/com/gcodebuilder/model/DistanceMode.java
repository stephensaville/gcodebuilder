package com.gcodebuilder.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum DistanceMode implements GCodeWord {
    ABSOLUTE("G90"),
    INCREMENTAL("G91");

    private final String gcode;

    @Override
    public String toGCode() {
        return gcode;
    }
}
