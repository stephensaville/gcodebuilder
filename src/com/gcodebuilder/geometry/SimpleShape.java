package com.gcodebuilder.geometry;

import java.util.Collections;
import java.util.List;

public abstract class SimpleShape<H> extends Shape<H> {
    public SimpleShape(Class<H> handleClass) {
        super(handleClass);
    }

    public abstract Path convertToPath();

    public List<Path> convertToPaths() {
        return Collections.singletonList(convertToPath());
    }
}
