package com.gcodebuilder.app;

import javafx.scene.control.SpinnerValueFactory;
import lombok.Getter;
import lombok.Setter;

class ExponentialIntegerSpinnerValueFactory extends SpinnerValueFactory.IntegerSpinnerValueFactory {
    @Getter @Setter
    private int base;

    public ExponentialIntegerSpinnerValueFactory(int minValue, int maxValue, int value, int base) {
        super(minValue, maxValue, value, 1);
        this.base = base;
    }

    public ExponentialIntegerSpinnerValueFactory(int minValue, int maxValue, int value) {
        this(minValue, maxValue, value, 2);
    }

    @Override
    public void decrement(int i) {
        setValue((int)(getValue() * Math.pow(base, -i)));
    }

    @Override
    public void increment(int i) {
        setValue((int)(getValue() * Math.pow(base, i)));
    }
}
