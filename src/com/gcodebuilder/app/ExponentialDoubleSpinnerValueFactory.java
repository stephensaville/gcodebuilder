package com.gcodebuilder.app;

import javafx.scene.control.SpinnerValueFactory;
import lombok.Getter;
import lombok.Setter;

class ExponentialDoubleSpinnerValueFactory extends SpinnerValueFactory.DoubleSpinnerValueFactory {
    @Getter @Setter
    private double base;

    public ExponentialDoubleSpinnerValueFactory(double minValue, double maxValue, double value, double base) {
        super(minValue, maxValue, value, 1);
        this.base = base;
    }

    public ExponentialDoubleSpinnerValueFactory(double minValue, double maxValue, double value) {
        this(minValue, maxValue, value, 2);
    }

    @Override
    public void decrement(int i) {
        setValue(getValue() * Math.pow(base, -i));
    }

    @Override
    public void increment(int i) {
        setValue(getValue() * Math.pow(base, i));
    }
}
