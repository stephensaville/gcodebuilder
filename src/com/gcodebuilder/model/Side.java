package com.gcodebuilder.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Side {
    INSIDE("Inside"),
    OUTSIDE("Outside");

    private final String label;
}
