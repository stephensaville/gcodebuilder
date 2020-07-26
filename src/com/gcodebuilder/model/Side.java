package com.gcodebuilder.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Side {
    INSIDE("Inside", -1),
    OUTSIDE("Outside", 1);

    private final String label;
    private final int offsetSign;
}
