package com.gcodebuilder.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class GCodeParamTest {

    @Test
    public void testX() {
        GCodeParam param = GCodeParam.X(1.0);
        assertEquals('X', param.getLetter());
        assertEquals(1.0, param.getValue());
        assertEquals("X1.0000", param.toGCode());
    }

}
