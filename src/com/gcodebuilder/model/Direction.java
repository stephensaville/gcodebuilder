package com.gcodebuilder.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Direction {
    CLOCKWISE("Clockwise"),
    COUNTER_CLOCKWISE("Counter Clockwise");

    private final String label;
}
