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
