package com.gcodebuilder.geometry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LineTest {

    @Test
    public void testComputeX() {
        Line line1 = new Line(0, 0, 1, 1);
        Assertions.assertEquals(-2, line1.calculateX(-2));
        Assertions.assertEquals(0, line1.calculateX(0));
        Assertions.assertEquals(2, line1.calculateX(2));

        Line line2 = new Line(1, 1, 0, 0);
        Assertions.assertEquals(-2, line2.calculateX(-2));
        Assertions.assertEquals(0, line2.calculateX(0));
        Assertions.assertEquals(2, line2.calculateX(2));

        Line line3 = new Line(-1, 2, 0, 1);
        Assertions.assertEquals(-1, line3.calculateX(2));
        Assertions.assertEquals(1, line3.calculateX(0));
        Assertions.assertEquals(3, line3.calculateX(-2));

        Line vertical = new Line(2, 0, 2, 1);
        Assertions.assertEquals(2, vertical.calculateX(-2));
        Assertions.assertEquals(2, vertical.calculateX(0));
        Assertions.assertEquals(2, vertical.calculateX(2));

        Line horizontal = new Line(2, 0, 3, 0);
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, horizontal.calculateX(-2));
        Assertions.assertEquals(2, horizontal.calculateX(0));
        Assertions.assertEquals(Double.POSITIVE_INFINITY, horizontal.calculateX(2));
    }
}
