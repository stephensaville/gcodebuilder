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

import com.google.common.base.Preconditions;
import lombok.Getter;

@Getter
public class GCodeParam implements GCodeWord {
    private final char letter;
    private final double value;

    private GCodeParam(char letter, double value) {
        this.letter = Character.toUpperCase(letter);
        this.value = value;
    }

    @Override
    public String toGCode() {
        return String.format("%c%.4f", letter, value);
    }

    public static GCodeParam X(double value) {
        return new GCodeParam('X', value);
    }

    public static GCodeParam Y(double value) {
        return new GCodeParam('Y', value);
    }

    public static GCodeParam Z(double value) {
        return new GCodeParam('Z', value);
    }

    public static GCodeParam A(double value) {
        return new GCodeParam('A', value);
    }

    public static GCodeParam B(double value) {
        return new GCodeParam('B', value);
    }

    public static GCodeParam C(double value) {
        return new GCodeParam('C', value);
    }

    public static GCodeParam U(double value) {
        return new GCodeParam('U', value);
    }

    public static GCodeParam V(double value) {
        return new GCodeParam('V', value);
    }

    public static GCodeParam W(double value) {
        return new GCodeParam('W', value);
    }

    public static GCodeParam I(double value) {
        return new GCodeParam('I', value);
    }

    public static GCodeParam J(double value) {
        return new GCodeParam('J', value);
    }

    public static GCodeParam K(double value) {
        return new GCodeParam('K', value);
    }

    public static GCodeParam P(double value) {
        return new GCodeParam('P', value);
    }

    public static GCodeParam Q(double value) {
        return new GCodeParam('Q', value);
    }

    public static GCodeParam R(double value) {
        return new GCodeParam('R', value);
    }
}
