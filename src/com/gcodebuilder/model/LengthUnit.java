package com.gcodebuilder.model;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import si.uom.SI;
import systems.uom.common.USCustomary;

import javax.measure.MetricPrefix;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.quantity.Length;
import java.util.EnumMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Getter
public enum LengthUnit {
    INCH("inch", UnitMode.INCH),
    MM("mm", UnitMode.MM);

    private final String label;
    private final UnitMode mode;

    public Unit<Length> getUnit() {
        return mode.getUnit();
    }

    public double getUnitsPerInch() {
        return mode.getUnitsPerInch();
    }

    private static final Map<UnitMode, LengthUnit> modeToUnit;
    static {
        modeToUnit = new EnumMap(UnitMode.class);
        for (LengthUnit unit : values()) {
            modeToUnit.put(unit.mode, unit);
        }
    }

    public static LengthUnit fromUnitMode(UnitMode mode) {
        return Preconditions.checkNotNull(modeToUnit.get(mode));
    }

    public String toString() {
        return label;
    }
}
