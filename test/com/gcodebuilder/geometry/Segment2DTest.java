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

import javafx.geometry.Point2D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class Segment2DTest {
    private static double DELTA = 1e-6;

    private static void assertEquals(Point2D expected, Point2D actual, double delta) {
        Assertions.assertEquals(expected.getX(), actual.getX(), delta);
        Assertions.assertEquals(expected.getY(), actual.getY(), delta);
    }

    @ParameterizedTest
    @MethodSource("projectPointTestArgs")
    public void testProjectPoint(PathSegment segment, Point2D point, Point2D expected) {
        Point2D actual = segment.project(point);
        assertEquals(expected, actual, DELTA);
    }

    private static Arguments projectPointTestArgs(double fromX, double fromY, double toX, double toY, double x,
                                                  double y, double expectedX, double expectedY) {
        return Arguments.of(LineSegment.of(fromX, fromY, toX, toY), new Point2D(x, y),
                new Point2D(expectedX, expectedY));
    }

    public static Stream<Arguments> projectPointTestArgs() {
        return Stream.of(
                projectPointTestArgs(0, 0, 0, 2, 1, 1, 0, 1),
                projectPointTestArgs(0, 0, 0, 2, -1, 1, 0, 1),
                projectPointTestArgs(0, 0, 0, 2, 1, -1, 0, -1),
                projectPointTestArgs(0, 0, 0, 2, -1, -1, 0, -1),
                projectPointTestArgs(0, 0, 2, 0, 1, 1, 1, 0),
                projectPointTestArgs(0, 0, 2, 0, 1, -1, 1, 0),
                projectPointTestArgs(0, 0, 2, 0, -1, 1, -1, 0),
                projectPointTestArgs(0, 0, 2, 0, -1, -1, -1, 0),
                projectPointTestArgs(1, 1, 3, 3, 1, 3, 2, 2),
                projectPointTestArgs(1, 1, 3, 3, 3, 1, 2, 2),
                projectPointTestArgs(1, 1, 3, 3, 1, 2, 1.5, 1.5),
                projectPointTestArgs(1, 1, 3, 3, 2, 1, 1.5, 1.5)
        );
    }
}
