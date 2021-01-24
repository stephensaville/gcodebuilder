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

import com.gcodebuilder.generator.toolpath.Toolpath;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Getter
public class LineSegment extends Line implements PathSegment {
    private static final Logger log = LogManager.getLogger(LineSegment.class);

    private final Point2D to;
    private final Point2D vector;
    private final double length;

    public static LineSegment of(Point2D from, Point2D to) {
        return new LineSegment(from, to, to.subtract(from));
    }

    public static PathSegment of(Point from, Point to) {
        return of(from.asPoint2D(), to.asPoint2D());
    }

    public static PathSegment of(double fromX, double fromY, double toX, double toY) {
        return of(new Point2D(fromX, fromY), new Point2D(toX, toY));
    }

    protected LineSegment(Point2D from, Point2D to, Point2D vector) {
        super(from, UnitVector.from(vector));
        this.to = to;
        this.vector = vector;
        this.length = vector.magnitude();
    }

    protected LineSegment(Point2D from, Point2D to, Point2D vector, UnitVector direction, double length) {
        super(from, direction);
        this.to = to;
        this.vector = vector;
        this.length = length;
    }

    public UnitVector getFromDirection() {
        return getDirection();
    }

    public double getLengthSquared() {
        return length * length;
    }

    @Override
    public LineSegment move(Point2D offset) {
        return new LineSegment(getFrom().add(offset), getTo().add(offset), getVector(), getDirection(), getLength());
    }

    @Override
    public LineSegment flip() {
        return new LineSegment(getTo(), getFrom(), getVector().multiply(-1), getDirection().invert(), getLength());
    }

    @Override
    public Toolpath.Segment computeToolpathSegment(double toolRadius, boolean leftSide) {
        UnitVector away = leftSide ? getDirection().leftNormal() : getDirection().rightNormal();
        return new Toolpath.Segment(move(away.multiply(toolRadius)), toolRadius, leftSide,
                new Toolpath.Connection(getFrom()), new Toolpath.Connection(getTo()));
    }

    @Override
    public SplitSegments split(Point2D splitPoint) {
        return new SplitSegments(LineSegment.of(getFrom(), splitPoint), LineSegment.of(splitPoint, getTo()));
    }

    @Override
    public Comparator<Point2D> splitPointComparator() {
        return (left, right) -> {
            double distanceToLeft = getFrom().distance(left);
            double distanceToRight = getFrom().distance(right);
            return Double.compare(distanceToLeft, distanceToRight);
        };
    }

    public List<IntersectionPoint> intersect(LineSegment other) {
        Point2D between = getTo().subtract(other.getTo());

        double denominator = Math2D.det(getVector(), other.getVector());
        double thisParam = Math2D.det(between, other.getVector()) / denominator;
        double otherParam = Math2D.det(getVector(), between) / denominator;
        boolean onSegments = thisParam > 0 && thisParam < 1 && otherParam > -1 && otherParam < 0;

        return Collections.singletonList(
                new IntersectionPoint(getTo().add(getVector().multiply(-thisParam)), onSegments));
    }

    @Override
    public List<IntersectionPoint> intersect(ArcSegment other) {
        return other.intersect(this);
    }

    @Override
    public Point2D project(Point2D point) {
        Point2D vectorToPoint = point.subtract(getFrom());
        double distToProjection = vectorToPoint.dotProduct(getDirection());
        if (distToProjection >= 0 && distToProjection <= length) {
            return getFrom().add(getDirection().multiply(distToProjection));
        } else {
            return null;
        }
    }

    @Override
    public boolean isWindingMatch(Point2D point) {
        // y range is [min(fromY, toY), max(fromY, toY)), which includes minY and excludes maxY
        // x range is (-infinity, segmentX), which excludes segmentX
        return (point.getY() < getFrom().getY() != point.getY() < getTo().getY()) &&
                point.getX() < calculateX(point.getY());
    }

    @Override
    public void draw(GraphicsContext ctx) {
        ctx.strokeLine(getFrom().getX(), getFrom().getY(), getTo().getX(), getTo().getY());
    }

    @Override
    public String toString() {
        return String.format("LineSegment((%s,%s), (%s,%s))",
                getFrom().getX(), getFrom().getY(), getTo().getX(), getTo().getY());
    }
}
