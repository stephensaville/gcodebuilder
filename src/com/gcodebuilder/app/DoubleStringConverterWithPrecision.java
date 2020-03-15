package com.gcodebuilder.app;

import javafx.util.StringConverter;

public class DoubleStringConverterWithPrecision extends StringConverter<Double> {
    private final double precisionFactor;

    public DoubleStringConverterWithPrecision(int precision) {
        precisionFactor = Math.pow(10, precision);
    }

    public double roundToPrecision(Double originalValue) {
        return Math.rint(originalValue * precisionFactor) / precisionFactor;
    }

    @Override
    public String toString(Double aDouble) {
        return Double.toString(roundToPrecision(aDouble));
    }

    @Override
    public Double fromString(String s) {
        return roundToPrecision(Double.valueOf(s));
    }
}
