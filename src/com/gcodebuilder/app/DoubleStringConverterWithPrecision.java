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
