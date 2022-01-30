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

package com.gcodebuilder.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum UnitMode implements GCodeWord {
    INCH("G20", 1),
    MM("G21", 25.4);

    private final String gcode;

    @Getter
    private final double unitsPerInch;

    @Override
    public String toGCode() {
        return gcode;
    }

    public LengthUnitConverter getConverterTo(UnitMode other) {
        if (other == this) {
            return new LengthUnitConverter(1);
        } else {
            return new LengthUnitConverter(other.unitsPerInch / unitsPerInch);
        }
    }
}
