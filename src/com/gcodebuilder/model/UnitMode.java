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

    public LengthUnitConverter getConverterTo(UnitMode other) {
        if (other == this) {
            return new LengthUnitConverter(1);
        } else {
            return new LengthUnitConverter(other.unitsPerInch / unitsPerInch);
        }
    }
}
