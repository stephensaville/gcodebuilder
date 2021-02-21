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

package com.gcodebuilder.generator.toolpath;

import com.gcodebuilder.geometry.ArcSegment;
import com.gcodebuilder.geometry.Math2D;
import com.gcodebuilder.geometry.Path;
import com.gcodebuilder.geometry.PathSegment;
import com.gcodebuilder.model.Direction;
import com.gcodebuilder.model.GCodeBuilder;
import com.gcodebuilder.model.MotionMode;
import javafx.geometry.Point2D;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Data
@RequiredArgsConstructor
public class Toolpath {
    private static final Logger log = LogManager.getLogger(Toolpath.class);

    @Data
    public static class SplitPoint {
        private final Point2D point;
        private final boolean fromSideValid;
        private final boolean toSideValid;
        private final Connection connection;
    }

    @Getter
    @RequiredArgsConstructor
    @ToString
    public static class Connection {
        private final Point2D connectionPoint;

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }

    @Getter
    @ToString
    public static class Segment {
        private final PathSegment segment;
        private final double toolRadius;
        private final boolean leftSide;

        @Setter
        private Connection fromConnection;

        @Setter
        private Connection toConnection;

        private final List<SplitPoint> splitPoints = new ArrayList<>();
        private boolean splitPointsSorted = true;

        public Segment(PathSegment segment, double toolRadius, boolean leftSide,
                       Connection fromConnection, Connection toConnection) {
            this.segment = segment;
            this.toolRadius = toolRadius;
            this.leftSide = leftSide;
            this.fromConnection = fromConnection;
            this.toConnection = toConnection;
        }

        public List<PathSegment.IntersectionPoint> intersect(Segment other) {
            return segment.intersect(other.segment);
        }

        public Point2D getFrom() {
            return segment.getFrom();
        }

        public Point2D getTo() {
            return segment.getTo();
        }

        public void generateGCode(GCodeBuilder builder, int feedRate) {
            Toolpath.generateGCode(builder, segment, feedRate);
        }

        public void split(Point2D splitPoint, boolean fromSideValid, boolean toSideValid, Connection connection) {
            splitPoints.add(new SplitPoint(splitPoint, fromSideValid, toSideValid, connection));
            splitPointsSorted = false;
        }

        public Segment flip() {
            return segment.flipToolpathSegment(this);
        }

        public void sortSplitPoints() {
            if (!splitPointsSorted) {
                splitPoints.sort(Comparator.comparing(SplitPoint::getPoint, segment.splitPointComparator()));
                splitPointsSorted = true;
            }
        }

        public List<Segment> getValidSegments() {
            if (splitPoints.isEmpty()) {
                return Collections.singletonList(this);
            }
            List<Segment> validSegments = new ArrayList<>();
            sortSplitPoints();
            PathSegment remaining = segment;
            boolean prevToSideValid = true;
            Toolpath.Connection prevConnection = fromConnection;
            for (SplitPoint splitPoint : splitPoints) {
                PathSegment.SplitSegments splitSegments = remaining.split(splitPoint.getPoint());
                if (prevToSideValid && splitPoint.isFromSideValid()) {
                    Segment validSegment = new Segment(
                            splitSegments.getFromSegment(),
                            toolRadius, leftSide,
                            prevConnection, splitPoint.getConnection());
                    validSegments.add(validSegment);
                }
                remaining = splitSegments.getToSegment();
                prevToSideValid = splitPoint.isToSideValid();
                prevConnection = splitPoint.getConnection();
            }
            if (prevToSideValid) {
                Segment validSegment = new Segment(
                        remaining,
                        toolRadius,
                        leftSide,
                        prevConnection, toConnection);
                validSegments.add(validSegment);
            }
            return validSegments;
        }
    }

    @Data
    public static class SegmentPair {
        private final Segment left;
        private final Segment right;
    }

    private final List<Segment> segments;

    @Getter @Setter
    private Toolpath next;

    private Direction direction;

    /**
     * Creates a toolpath following the given path.
     *
     * @param path path to follow
     */
    public Toolpath(Path path, double toolRadius, boolean leftSide) {
        segments = path.getSegments().stream()
                .map(pathSegment -> new Segment(pathSegment, toolRadius, leftSide,
                        new Connection(pathSegment.getFrom()), new Connection(pathSegment.getTo())))
                .collect(Collectors.toList());
    }

    public boolean hasNext() {
        return next != null;
    }

    public Segment getFirstSegment() {
        return segments.get(0);
    }

    public Segment getLastSegment() {
        return segments.get(segments.size() - 1);
    }

    public Direction getDirection() {
        if (direction == null) {
            double totalAngleDiff = 0.0;
            Toolpath.Segment prevSegment = getLastSegment();
            for (Toolpath.Segment segment : segments) {
                totalAngleDiff += Math2D.subtractAngle(segment.getSegment().getFromAngle(),
                        prevSegment.getSegment().getFromAngle());
                prevSegment = segment;
            }
            return (totalAngleDiff > 0) ? Direction.COUNTER_CLOCKWISE : Direction.CLOCKWISE;
        }
        return direction;
    }

    public Toolpath reverse() {
        List<Toolpath.Segment> reversed = new ArrayList<>(segments.size());
        for (int i = segments.size() - 1; i >= 0; --i) {
            reversed.add(segments.get(i).flip());
        }
        return new Toolpath(reversed);
    }

    public Toolpath orient(Direction direction) {
        if (direction == Direction.ORIGINAL) {
            return this;
        } else if (direction == getDirection()) {
            return this;
        } else {
            Toolpath oriented = reverse();
            oriented.direction = direction;
            return oriented;
        }
    }

    public static void generateGCode(GCodeBuilder builder, PathSegment segment, int feedRate) {
        if (segment instanceof ArcSegment) {
            ArcSegment arcSegment = (ArcSegment)segment;
            if (arcSegment.isClockwise()) {
                builder.motionMode(MotionMode.CW_ARC);
            } else {
                builder.motionMode(MotionMode.CCW_ARC);
            }
            Point2D centerOffset = arcSegment.getCenter().subtract(arcSegment.getFrom());
            builder.feedRate(feedRate)
                    .XY(arcSegment.getTo().getX(), arcSegment.getTo().getY())
                    .IJ(centerOffset.getX(), centerOffset.getY())
                    .endLine();
        } else {
            builder.motionMode(MotionMode.LINEAR).feedRate(feedRate)
                    .XY(segment.getTo().getX(), segment.getTo().getY())
                    .endLine();
        }
    }
}
