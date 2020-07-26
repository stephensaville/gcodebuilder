package com.gcodebuilder.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GCodeComment implements GCodeWord {
    @Getter
    private final String text;

    @Override
    public String toGCode() {
        return String.format("( %s )", text);
    }
}
