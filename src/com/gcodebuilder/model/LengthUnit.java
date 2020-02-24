package com.gcodebuilder.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum LengthUnit {
    INCH("inch"),
    MM("mm");

    private final String label;

    public String toString() {
        return label;
    }
}
