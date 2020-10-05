package com.gcodebuilder.geometry;

import lombok.Data;

@Data
public abstract class Range {
    protected final double min;
    protected final double max;

    public abstract boolean includes(double value);

    protected Range(double min, double max) {
        assert min <= max;
        this.min = min;
        this.max = max;
    }

    public static Range exclusiveMinAndMax(double min, double max) {
        return new Range(min, max) {
            public boolean includes (double value){
                return min < value && value < max;
            }
        };
    }

    public static Range inclusiveMinExclusiveMax(double min, double max) {
        return new Range(min, max) {
            public boolean includes(double value) {
                return min <= value && value < max;
            }
        };
    }

    public static Range exclusiveMinInclusiveMax(double min, double max) {
        return new Range(min, max) {
            public boolean includes(double value) {
                return min < value && value <= max;
            }
        };
    }

    public static Range inclusiveMinAndMax(double min, double max) {
        return new Range(min, max) {
            public boolean includes(double value) {
                return min <= value && value <= max;
            }
        };
    }
}
