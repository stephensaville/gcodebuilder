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

import com.google.common.collect.ImmutableList;
import javafx.geometry.Point2D;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * Triangle represents a triangle in the 2D plane as its three vertices (pointA, pointB, pointC), internal angle at each
 * vertex (angleA, angleB, angleC) and length of the side opposite each vertex (lengthA, lengthB, lengthC). Triangle
 * is primary used to solve various triangle problems where only some of a triangle's features are known and need to be
 * calculated based on the known features. The result of each of these find functions is a list of zero or more
 * triangles matching the known features.
 */
@Data
public class Triangle {
    private final Point2D pointA;
    private final Point2D pointB;
    private final Point2D pointC;

    public static double getAngle(Point2D point, Point2D adjPointA, Point2D adjPointB) {
        UnitVector vecA = UnitVector.from(point, adjPointA);
        UnitVector vecB = UnitVector.from(point, adjPointB);
        return vecA.absAngleDiff(vecB);
    }

    public double getAngleA() {
        return getAngle(pointA, pointB, pointC);
    }

    public double getAngleB() {
        return getAngle(pointB, pointA, pointC);
    }

    public double getAngleC() {
        return getAngle(pointC, pointA, pointB);
    }

    public static double getLength(Point2D from, Point2D to) {
        return from.distance(to);
    }

    public double getLengthA() {
        return getLength(pointB, pointC);
    }

    public double getLengthB() {
        return getLength(pointA, pointC);
    }

    public double getLengthC() {
        return getLength(pointA, pointB);
    }

    public static Triangle find(Point2D pointA, Point2D pointB, double angleB, double lengthB,
                                UnitVector vecBC, double angleC) {
        double angleA = Math.PI - angleB - angleC;
        double lengthA = lengthB * Math.sin(angleA) / Math.sin(angleB);
        Point2D pointC = pointB.add(vecBC.multiply(lengthA));
        return new Triangle(pointA, pointB, pointC);
    }

    public static List<Triangle> find(Point2D pointA, Point2D pointB, UnitVector vecBC, double lengthB) {
        double lengthC = getLength(pointA, pointB);
        UnitVector vecBA = UnitVector.from(pointB, pointA);
        double angleB = vecBA.absAngleDiff(vecBC);
        if (Math2D.sameAngles(angleB, Math.PI)) {
            // special case: straight line from A to B to C
            Point2D pointC = pointA.add(vecBC.multiply(lengthB));
            return ImmutableList.of(new Triangle(pointA, pointB, pointC));
        }
        double sinAngleC = (lengthC / lengthB) * Math.sin(angleB);
        if (sinAngleC > 1) {
            return ImmutableList.of();
        } else if (sinAngleC == 1) {
            // angle C is a right angle
            double lengthA = Math.sqrt(lengthB*lengthB - lengthC*lengthC);
            Point2D pointC = pointB.add(vecBC.multiply(lengthA));
            return ImmutableList.of(new Triangle(pointA, pointB, pointC));
        } else if (lengthB >= lengthC)  {
            // only one triangle is possible
            double angleC = Math.asin(sinAngleC);
            return ImmutableList.of(find(pointA, pointB, angleB, lengthB, vecBC, angleC));
        } else {
            // two triangles are possible
            double angleC = Math.asin(sinAngleC);
            double altAngleC = Math.PI - angleC;
            return ImmutableList.of(
                    find(pointA, pointB, angleB, lengthB, vecBC, angleC),
                    find(pointA, pointB, angleB, lengthB, vecBC, altAngleC));
        }
    }

    public static Triangle find(Point2D pointA, Point2D pointB, double lengthB, UnitVector vecAC) {
        Point2D pointC = pointA.add(vecAC.multiply(lengthB));
        return new Triangle(pointA, pointB, pointC);
    }

    public static List<Triangle> find(Point2D pointA, double lengthA, Point2D pointB, double lengthB) {
        double lengthC = getLength(pointA, pointB);
        double angleA = Math.acos((lengthB*lengthB + lengthC*lengthC - lengthA*lengthA)/(2*lengthB*lengthC));
        UnitVector vecAB = UnitVector.from(pointA, pointB);
        return ImmutableList.of(
                find(pointA, pointB, lengthB, vecAB.rotate(angleA)),
                find(pointA, pointB, lengthB, vecAB.rotate(-angleA)));
    }
}
