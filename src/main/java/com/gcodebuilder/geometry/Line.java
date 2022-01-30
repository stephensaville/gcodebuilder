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
import lombok.Data;

@Data
public class Line {
    private final Point2D from;
    private final UnitVector direction;

    public Line(Point2D from, UnitVector direction) {
        this.from = from;
        this.direction = direction;
    }

    public Line(Point2D from, Point2D to) {
        this(from, UnitVector.from(from, to));
    }

    public Line(double fromX, double fromY, double toX, double toY) {
        this(new Point2D(fromX, fromY), UnitVector.from(toX - fromX, toY - fromY));
    }

    public Point2D project(Point2D point) {
        Point2D vectorToPoint = point.subtract(from);
        double distToProjection = vectorToPoint.dotProduct(direction);
        return from.add(direction.multiply(distToProjection));
    }

    public double getFromAngle() {
        return getDirection().getAngle();
    }

    /**
     * Computes X coordinate of a point on the line with the given Y coordinate.
     *
     * @param y y coordinate of point on line
     * @return x coordinate of point on line
     */
    public double calculateX(double y) {
        double yOffset = y - from.getY();
        if (direction.getY() != 0) {
            return from.getX() + yOffset * direction.getX() / direction.getY();
        } else if (yOffset > 0) {
            return Double.POSITIVE_INFINITY;
        } else if (yOffset < 0) {
            return Double.NEGATIVE_INFINITY;
        } else {
            return from.getX();
        }
    }

    @Override
    public String toString() {
        return String.format("Line(from=(%f,%f), direction=(%f,%f))",
                getFrom().getY(), getFrom().getY(), getDirection().getX(), getDirection().getY());
    }
}
