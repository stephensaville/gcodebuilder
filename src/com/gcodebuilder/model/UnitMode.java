package com.gcodebuilder.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import si.uom.SI;
import systems.uom.common.USCustomary;

import javax.measure.MetricPrefix;
import javax.measure.Unit;
import javax.measure.quantity.Length;

@RequiredArgsConstructor
public enum UnitMode implements GCodeWord {
    INCH("G20", 1, USCustomary.INCH),
    MM("G21", 25.4, MetricPrefix.MILLI(SI.METRE));

    private final String gcode;

    @Getter
    private final double unitsPerInch;

    @Getter
    private final Unit<Length> unit;

    @Override
    public String toGCode() {
        return gcode;
    }
}
