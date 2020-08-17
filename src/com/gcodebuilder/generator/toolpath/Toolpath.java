package com.gcodebuilder.generator.toolpath;

import com.gcodebuilder.geometry.UnitVector;
import javafx.geometry.Point2D;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
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
        private final com.gcodebuilder.geometry.Segment segment;
        private final double toolRadius;
        private final UnitVector towards;
        private final UnitVector away;

        @Setter
        private Connection fromConnection;

        @Setter
        private Connection toConnection;

        private final List<SplitPoint> splitPoints = new ArrayList<>();
        private boolean splitPointsSorted = true;

        public Segment(com.gcodebuilder.geometry.Segment segment, double toolRadius, UnitVector towards, UnitVector away,
                       Connection fromConnection, Connection toConnection) {
            this.segment = segment;
            this.toolRadius = toolRadius;
            this.towards = towards;
            this.away = away;
            this.fromConnection = fromConnection;
            this.toConnection = toConnection;
        }

        public static Segment fromEdge(com.gcodebuilder.geometry.Segment edge, double toolRadius, UnitVector towards, UnitVector away) {
            return new Segment(edge.move(away.multiply(toolRadius)), toolRadius, towards, away,
                    new Connection(edge.getFrom()), new Connection(edge.getTo()));
        }

        public Point2D intersect(Segment other) {
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
            List<Segment> validSegments = new ArrayList<>();
            sortSplitPoints();
            SplitPoint prevSplitPoint = new SplitPoint(segment.getFrom(),
                true, true, fromConnection);
            for (SplitPoint splitPoint : splitPoints) {
                if (prevSplitPoint.isToSideValid() && splitPoint.isFromSideValid()) {
                    Segment validSegment = new Segment(
                            com.gcodebuilder.geometry.Segment.of(prevSplitPoint.getPoint(), splitPoint.getPoint()),
                            toolRadius, towards, away, prevSplitPoint.getConnection(),
                            splitPoint.getConnection());
                    validSegments.add(validSegment);
                }
                prevSplitPoint = splitPoint;
            }
            if (prevSplitPoint.isToSideValid()) {
                Segment validSegment = new Segment(
                        com.gcodebuilder.geometry.Segment.of(prevSplitPoint.getPoint(), segment.getTo()),
                        toolRadius, towards, away, prevSplitPoint.getConnection(),
                        toConnection);
                validSegments.add(validSegment);
            }
            return validSegments;
        }
    }

    private final List<Segment> segments;

    public Segment getFirstSegment() {
        return segments.get(0);
    }

    public Segment getLastSegment() {
        return segments.get(segments.size() - 1);
    }
}
