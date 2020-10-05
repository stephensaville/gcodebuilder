package com.gcodebuilder.generator.toolpath;

import com.gcodebuilder.geometry.Math2D;
import com.gcodebuilder.geometry.PathSegment;
import com.gcodebuilder.geometry.UnitVector;
import com.gcodebuilder.model.Direction;
import javafx.geometry.Point2D;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Data
public class Toolpath {
    @Data
    public static class SplitPoint {
        private final Point2D point;
        private final boolean fromSideValid;
        private final boolean toSideValid;
        private final Connection connection;
    }

    @RequiredArgsConstructor
    public static class SplitPointComparator implements Comparator<SplitPoint> {
        private final Point2D from;

        @Override
        public int compare(SplitPoint left, SplitPoint right) {
            double leftDistance = from.distance(left.getPoint());
            double rightDistance = from.distance(right.getPoint());
            return Double.compare(leftDistance, rightDistance);
        }
    }

    @Getter
    @RequiredArgsConstructor
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
    public static class Segment {
        private final PathSegment segment;
        private final double toolRadius;
        private final UnitVector towards;
        private final UnitVector away;

        @Setter
        private Connection fromConnection;

        @Setter
        private Connection toConnection;

        private final List<SplitPoint> splitPoints = new ArrayList<>();
        private boolean splitPointsSorted = true;

        public Segment(PathSegment segment, double toolRadius, UnitVector towards, UnitVector away,
                       Connection fromConnection, Connection toConnection) {
            this.segment = segment;
            this.toolRadius = toolRadius;
            this.towards = towards;
            this.away = away;
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

        public void split(Point2D splitPoint, boolean fromSideValid, boolean toSideValid, Connection connection) {
            splitPoints.add(new SplitPoint(splitPoint, fromSideValid, toSideValid, connection));
            splitPointsSorted = false;
        }

        public Segment flip() {
            return new Segment(segment.flip(), toolRadius, towards, away, toConnection, fromConnection);
        }

        public void sortSplitPoints() {
            if (!splitPointsSorted) {
                splitPoints.sort(new SplitPointComparator(segment.getFrom()));
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
                            toolRadius, towards, away,
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
                        toolRadius, towards, away,
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
                totalAngleDiff += Math2D.subtractAngle(segment.getSegment().getAngle(),
                        prevSegment.getSegment().getAngle());
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
        if (direction == getDirection()) {
            return this;
        } else {
            Toolpath oriented = reverse();
            oriented.direction = direction;
            return oriented;
        }
    }
}
