package com.gcodebuilder.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum LengthUnit {
    INCH("inch", UnitMode.INCH),
    MM("mm", UnitMode.MM);

    private final String label;
    private final UnitMode mode;

    public String toString() {
        return label;
    }
}
