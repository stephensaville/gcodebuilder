/*
 * Copyright (c) 2021 Stephen Saville
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gcodebuilder.geometry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Math2DTest {

    private static final double SQRT_HALF = Math.sqrt(0.5);

    @Test
    public void testComputeAngle() {
        Assertions.assertEquals(0.0, Math2D.computeAngle(1.0, 0.0));
        Assertions.assertEquals(Math.PI/4, Math2D.computeAngle(SQRT_HALF, SQRT_HALF));
        Assertions.assertEquals(Math.PI/2, Math2D.computeAngle(0.0, 1.0));
        Assertions.assertEquals(3*Math.PI/4, Math2D.computeAngle(-SQRT_HALF, SQRT_HALF));
        Assertions.assertEquals(Math.PI, Math2D.computeAngle(-1.0, 0.0));
        Assertions.assertEquals(5*Math.PI/4, Math2D.computeAngle(-SQRT_HALF, -SQRT_HALF));
        Assertions.assertEquals(3*Math.PI/2, Math2D.computeAngle(0.0, -1.0));
        Assertions.assertEquals(7*Math.PI/4, Math2D.computeAngle(SQRT_HALF, -SQRT_HALF));
    }

    @Test
    public void testAddAngle() {
        Assertions.assertEquals(0, Math2D.addAngle(0, 2*Math.PI));
        Assertions.assertEquals(0, Math2D.addAngle(0, -2*Math.PI));
        Assertions.assertEquals(0, Math2D.addAngle(Math.PI, Math.PI));
        Assertions.assertEquals(0, Math2D.addAngle(Math.PI, -Math.PI));
        Assertions.assertEquals(Math.PI, Math2D.addAngle(0, Math.PI));
        Assertions.assertEquals(Math.PI, Math2D.addAngle(0, -Math.PI));
        Assertions.assertEquals(Math.PI, Math2D.addAngle(Math.PI, 2*Math.PI));
        Assertions.assertEquals(Math.PI, Math2D.addAngle(Math.PI, -2*Math.PI));
        Assertions.assertEquals(Math.PI/2, Math2D.addAngle(0, Math.PI/2));
        Assertions.assertEquals(Math.PI/2, Math2D.addAngle(0, -3*Math.PI/2));
        Assertions.assertEquals(Math.PI/2, Math2D.addAngle(Math.PI, -Math.PI/2));
        Assertions.assertEquals(Math.PI/2, Math2D.addAngle(Math.PI, 3*Math.PI/2));
        Assertions.assertEquals(3*Math.PI/2, Math2D.addAngle(0, 3*Math.PI/2));
        Assertions.assertEquals(3*Math.PI/2, Math2D.addAngle(0, -Math.PI/2));
        Assertions.assertEquals(3*Math.PI/2, Math2D.addAngle(Math.PI, Math.PI/2));
        Assertions.assertEquals(3*Math.PI/2, Math2D.addAngle(Math.PI, -3*Math.PI/2));
    }

    @Test
    public void testSubtractAngle() {
        Assertions.assertEquals(Math.PI/2, Math2D.subtractAngle(Math.PI/2, 0));
        Assertions.assertEquals(Math.PI/2, Math2D.subtractAngle(Math.PI, Math.PI/2));
        Assertions.assertEquals(Math.PI/2, Math2D.subtractAngle(3*Math.PI/2, Math.PI));
        Assertions.assertEquals(Math.PI/2, Math2D.subtractAngle(0, 3*Math.PI/2));
        Assertions.assertEquals(-Math.PI/2, Math2D.subtractAngle(3*Math.PI/2, 0));
        Assertions.assertEquals(-Math.PI/2, Math2D.subtractAngle(0, Math.PI/2));
        Assertions.assertEquals(-Math.PI/2, Math2D.subtractAngle(Math.PI/2, Math.PI));
        Assertions.assertEquals(-Math.PI/2, Math2D.subtractAngle(Math.PI, 3*Math.PI/2));
        Assertions.assertEquals(Math.PI, Math2D.subtractAngle(Math.PI, 0));
        Assertions.assertEquals(Math.PI, Math2D.subtractAngle(3*Math.PI/2, Math.PI/2));
        Assertions.assertEquals(Math.PI, Math2D.subtractAngle(0, Math.PI));
    }

}
