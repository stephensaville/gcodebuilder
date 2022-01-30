/*
 * Copyright (c) 2021 Stephen Saville
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
