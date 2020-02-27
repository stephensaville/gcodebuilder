package com.gcodebuilder.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class FeedRate implements GCodeWord {
    private final int rate;

    @Override
    public String toGCode() {
        return String.format("%c%d", 'F', rate);
    }
}
