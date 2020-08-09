package com.gcodebuilder.model;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LengthUnitConverter {
    private final double conversionFactor;

    public double convert(double length) {
        return length * conversionFactor;
    }
}
