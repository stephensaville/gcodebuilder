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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.geometry.Point2D;

public class Point {

    public enum Type {
        CW_CENTER,
        CCW_CENTER
    }

    private final Point2D point2D;
    private final Type type;

    @JsonCreator
    public Point(@JsonProperty("x") double x,
                 @JsonProperty("y") double y,
                 @JsonProperty("t") Type type) {
        this.point2D = new Point2D(x, y);
        this.type = type;
    }

    public Point(double x, double y) {
        this(x, y, null);
    }

    public Point(Point2D point2D, Type type) {
        this.point2D = point2D;
        this.type = type;
    }

    public Point(Point2D point2D) {
        this(point2D, null);
    }

    public Point2D asPoint2D() {
        return point2D;
    }

    @JsonProperty
    public double getX() {
        return point2D.getX();
    }

    @JsonProperty
    public double getY() {
        return point2D.getY();
    }

    @JsonProperty("t")
    public Type getType() {
        return type;
    }

    @JsonIgnore
    public boolean isCenterPoint() {
        return type == Type.CW_CENTER || type == Type.CCW_CENTER;
    }

    @JsonIgnore
    public boolean isClockwiseCenterPoint() {
        return type == Type.CW_CENTER;
    }

    public double distance(Point2D point2D) {
        return this.point2D.distance(point2D);
    }

    public double distance(Point otherPoint) {
        return distance(otherPoint.asPoint2D());
    }

    public Point add(Point2D point2D) {
        return new Point(this.point2D.add(point2D), type);
    }

    public boolean isSame(Point otherPoint, double maxDistance) {
        if (otherPoint == null) {
            return false;
        } else if (this.equals(otherPoint)) {
            return true;
        } else {
            return this.distance(otherPoint) < maxDistance;
        }
    }

    public boolean isSame(Point otherPoint) {
        return isSame(otherPoint, Math2D.SAME_POINT_DISTANCE);
    }

    public boolean isSame(Point2D point2D, double maxDistance) {
        if (point2D == null) {
            return false;
        } else {
            return this.distance(point2D) < maxDistance;
        }
    }

    public boolean isSame(Point2D point2D) {
        return isSame(point2D, Math2D.SAME_POINT_DISTANCE);
    }

    public String toCoordinateString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(getX());
        sb.append(", ");
        sb.append(getY());
        if (getType() != null) {
            sb.append(", ");
            sb.append(getType());
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Point" + toCoordinateString();
    }
}

