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

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum FeedRateMode implements GCodeWord {
    INVERSE_TIME("G93"),
    UNITS_PER_MIN("G94"),
    UNITS_PER_REV("G95");

    private final String gcode;

    @Override
    public String toGCode() {
        return gcode;
    }
}
