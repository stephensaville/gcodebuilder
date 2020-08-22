package com.gcodebuilder.model;

import lombok.Data;

@Data
public class FeedRate implements GCodeWord {
    private final int rate;

    @Override
    public String toGCode() {
        return String.format("%c%d", 'F', rate);
    }
}
