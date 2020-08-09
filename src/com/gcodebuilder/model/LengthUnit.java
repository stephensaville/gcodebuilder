package com.gcodebuilder.model;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.EnumMap;
import java.util.Map;

@RequiredArgsConstructor
@Getter
public enum LengthUnit {
    INCH("inch", UnitMode.INCH),
    MM("mm", UnitMode.MM);

    private final String label;
    private final UnitMode mode;

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

    public LengthUnitConverter getConverterTo(UnitMode other) {
        return mode.getConverterTo(other);
    }

    public LengthUnitConverter getConverterTo(LengthUnit other) {
        return getConverterTo(other.mode);
    }

    public String toString() {
        return label;
    }
}
