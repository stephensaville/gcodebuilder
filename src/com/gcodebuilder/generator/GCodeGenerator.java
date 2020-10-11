package com.gcodebuilder.generator;

import com.gcodebuilder.model.GCodeBuilder;
import com.gcodebuilder.model.GCodeProgram;

public interface GCodeGenerator {
    void generateGCode(GCodeBuilder builder);

    default GCodeProgram generateGCode() {
        GCodeBuilder builder = new GCodeBuilder();
        generateGCode(builder);
        return builder.build();
    }
}
