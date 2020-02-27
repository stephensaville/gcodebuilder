package com.gcodebuilder.generator;

import com.gcodebuilder.model.GCodeBuilder;

public interface GCodeGenerator {
    void generateGCode(GCodeBuilder builder);
}
