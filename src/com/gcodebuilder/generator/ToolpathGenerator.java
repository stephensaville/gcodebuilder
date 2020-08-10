package com.gcodebuilder.generator;

import com.gcodebuilder.geometry.Math2D;
import com.gcodebuilder.geometry.Path;
import com.gcodebuilder.geometry.Point;
import com.gcodebuilder.geometry.Segment;
import com.gcodebuilder.geometry.UnitVector;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

public class ToolpathGenerator {
    private static final Paint PATH_PAINT = Color.BLACK;
    private static final Paint VALID_PAINT = Color.GREEN;
    private static final Paint INVALID_PAINT = Color.RED;
    private static final Paint INSIDE_PAINT = Color.PURPLE;
    private static final Paint OUTSIDE_PAINT = Color.DARKORANGE;
    private static final Paint FROM_PAINT = Color.CYAN;
    private static final Paint TO_PAINT = Color.MAGENTA;

    @Getter @Setter
    private double pointRadius = 5;

    @Getter @Setter
    private double toolRadius = 50;

    private static final double MIN_POINT_DISTANCE = 0.0001;

    private List<Path> paths = new ArrayList<>();

    public void addPath(Path path) {
        paths.add(path);
    }

    public boolean removePath(Path path) {
        return paths.remove(path);
    }

    public void clearPaths() {
        paths.clear();
    }

    @Data
    private static class ToolpathSplitPoint {
        private final Point2D point;
        private final boolean fromSideValid;
        private final boolean toSideValid;
        private final ToolpathConnection connection;
    }

    @RequiredArgsConstructor
    private static class ToolpathSplitPointComparator implements Comparator<ToolpathSplitPoint> {
        private final Point2D from;

        @Override
        public int compare(ToolpathSplitPoint left, ToolpathSplitPoint right) {
            double leftDistance = from.distance(left.getPoint());
            double rightDistance = from.distance(right.getPoint());
            return Double.compare(leftDistance, rightDistance);
        }
    }

    // NOTE: must not use @Data because this class uses instance identity
    @Getter
    @RequiredArgsConstructor
    private static class ToolpathConnection {
        private final Point2D connectionPoint;
    }

    @Getter
    private static class ToolpathSegment {
        private final Segment segment;
        private final double toolRadius;
        private final UnitVector towards;
        private final UnitVector away;

        @Setter
        private ToolpathConnection fromConnection;

        @Setter
        private ToolpathConnection toConnection;

        private final List<ToolpathSplitPoint> splitPoints = new ArrayList<>();
        private boolean splitPointsSorted = true;

        public ToolpathSegment(Segment segment, double toolRadius, UnitVector towards, UnitVector away,
                               ToolpathConnection fromConnection, ToolpathConnection toConnection) {
            this.segment = segment;
            this.toolRadius = toolRadius;
            this.towards = towards;
            this.away = away;
            this.fromConnection = fromConnection;
            this.toConnection = toConnection;
        }

        public static ToolpathSegment fromEdge(Segment edge, double toolRadius, UnitVector towards, UnitVector away) {
            return new ToolpathSegment(edge.move(away.multiply(toolRadius)), toolRadius, towards, away,
                    new ToolpathConnection(edge.getFrom()), new ToolpathConnection(edge.getTo()));
        }

        public Point2D intersect(ToolpathSegment other) {
            return segment.intersect(other.segment);
        }

        public Point2D getFrom() {
            return segment.getFrom();
        }

        public Point2D getTo() {
            return segment.getTo();
        }

        public void split(Point2D splitPoint, boolean fromSideValid, boolean toSideValid, ToolpathConnection connection) {
            splitPoints.add(new ToolpathSplitPoint(splitPoint, fromSideValid, toSideValid, connection));
            splitPointsSorted = false;
        }

        public ToolpathSegment flip() {
            return new ToolpathSegment(segment.flip(), toolRadius, towards, away, toConnection, fromConnection);
        }

        public void sortSplitPoints() {
            if (!splitPointsSorted) {
                splitPoints.sort(new ToolpathSplitPointComparator(segment.getFrom()));
                splitPointsSorted = true;
            }
        }

        public List<ToolpathSegment> getValidSegments() {
            List<ToolpathSegment> validSegments = new ArrayList<>();
            sortSplitPoints();
            ToolpathSplitPoint prevSplitPoint = new ToolpathSplitPoint(segment.getFrom(),
                true, true, fromConnection);
            for (ToolpathSplitPoint splitPoint : splitPoints) {
                if (prevSplitPoint.isToSideValid() && splitPoint.isFromSideValid()) {
                    ToolpathSegment validSegment = new ToolpathSegment(
                            Segment.of(prevSplitPoint.getPoint(), splitPoint.getPoint()),
                            toolRadius, towards, away, prevSplitPoint.getConnection(),
                            splitPoint.getConnection());
                    validSegments.add(validSegment);
                }
                prevSplitPoint = splitPoint;
            }
            if (prevSplitPoint.isToSideValid()) {
                ToolpathSegment validSegment = new ToolpathSegment(
                        Segment.of(prevSplitPoint.getPoint(), segment.getTo()),
                        toolRadius, towards, away, prevSplitPoint.getConnection(),
                        toConnection);
                validSegments.add(validSegment);
            }
            return validSegments;
        }
    }

    private ToolpathSegment[] computeToolpathSegments(Segment edge, double toolRadius) {
        UnitVector left = edge.getDirection().leftNormal();
        UnitVector right = edge.getDirection().rightNormal();

        return new ToolpathSegment[] {
                ToolpathSegment.fromEdge(edge, toolRadius, right, left),
                ToolpathSegment.fromEdge(edge, toolRadius, left, right)
        };
    }

    private void connectToolpathSegments(List<ToolpathSegment> sameSideSegments) {
        ToolpathSegment prev = sameSideSegments.get(sameSideSegments.size() - 1);
        for (ToolpathSegment current : sameSideSegments) {
            prev.setToConnection(current.getFromConnection());
            prev = current;
        }
    }

    private void intersectToolpathSegments(int i, List<ToolpathSegment> toolpathSegments) {
        ToolpathSegment current = toolpathSegments.get(i);
        for (int j = i + 1; j < toolpathSegments.size(); ++j) {
            ToolpathSegment other = toolpathSegments.get(j);
            Point2D intersectionPoint = current.intersect(other);
            if (intersectionPoint != null) {
                ToolpathConnection connection = new ToolpathConnection(intersectionPoint);

                double angleFromCurrentToTowards = Math.abs(Math2D.subtractAngle(
                        other.getTowards().getAngle(), current.getSegment().getAngle()));
                boolean currentFromSideValid = (angleFromCurrentToTowards <= Math.PI/2);
                current.split(intersectionPoint, currentFromSideValid, !currentFromSideValid, connection);

                double angleFromOtherToTowards = Math.abs(Math2D.subtractAngle(
                        current.getTowards().getAngle(), other.getSegment().getAngle()));
                boolean otherFromSideValid = (angleFromOtherToTowards <= Math.PI/2);
                other.split(intersectionPoint, otherFromSideValid, !otherFromSideValid, connection);
            }
        }
    }

    private void intersectWithCorner(ToolpathSegment current, ToolpathSegment next,
                                     List<ToolpathSegment> toolpathSegments) {
        // skip inside corners
        Point2D currentNextIntersection = current.intersect(next);
        if (currentNextIntersection != null) {
            return;
        }

        Point2D cornerPoint = current.getToConnection().getConnectionPoint();
        if (cornerPoint == null) {
            return;
        }

        double toolRadius = current.getToolRadius();
        double toolRadiusSquared = toolRadius * toolRadius;

        Segment arcCurrent = Segment.of(cornerPoint, current.getTo());
        Segment arcNext = Segment.of(cornerPoint, next.getFrom());
        double arcAngle = Math2D.subtractAngle(arcNext.getAngle(), arcCurrent.getAngle());
        Segment arcBegin, arcEnd;
        if (arcAngle > 0) {
            arcBegin = arcCurrent;
            arcEnd = arcNext;
        } else {
            arcBegin = arcNext;
            arcEnd = arcCurrent;
            arcAngle = -arcAngle;
        }

        for (ToolpathSegment otherSegment : toolpathSegments) {
            if (otherSegment == current || otherSegment == next) {
                // skip current and next
                continue;
            }

            Point2D projectionPoint = otherSegment.getSegment().project(cornerPoint);
            if (projectionPoint == null) {
                // projected corner outside segment
                continue;
            }

            Segment cornerToProjection = Segment.of(cornerPoint, projectionPoint);
            double cornerToProjectionLengthSquared = Math2D.lengthSquared(cornerToProjection.getVector());
            if (cornerToProjectionLengthSquared >= toolRadiusSquared) {
                // corner too far from segment
                continue;
            }

            Segment cornerToFrom = Segment.of(cornerPoint, otherSegment.getFrom());
            double angleToFrom = Math2D.subtractAngle(cornerToFrom.getAngle(), cornerToProjection.getAngle());
            boolean fromToLeft = (angleToFrom > 0);

            double offsetToIntersection = Math.sqrt(toolRadiusSquared - cornerToProjectionLengthSquared);

            Point2D leftIntersectionPoint = projectionPoint.add(
                    cornerToProjection.getDirection().leftNormal().multiply(offsetToIntersection));
            Segment cornerToLeftIntersection = Segment.of(cornerPoint, leftIntersectionPoint);
            double angleToLeftIntersection = Math2D.subtractAngle(cornerToLeftIntersection.getAngle(), arcBegin.getAngle());
            if (angleToLeftIntersection > 0 && angleToLeftIntersection < arcAngle) {
                // left intersection inside corner arc
                ToolpathConnection connection = new ToolpathConnection(cornerPoint);
                otherSegment.split(leftIntersectionPoint, fromToLeft, !fromToLeft, connection);
                if (arcEnd == arcCurrent) {
                    current.setToConnection(connection);
                } else {
                    next.setFromConnection(connection);
                }
            }

            Point2D rightToIntersectionPoint = projectionPoint.add(
                    cornerToProjection.getDirection().rightNormal().multiply(offsetToIntersection));
            Segment cornerToRightIntersection = Segment.of(cornerPoint, rightToIntersectionPoint);
            double angleToRightIntersection = Math2D.subtractAngle(cornerToRightIntersection.getAngle(), arcBegin.getAngle());
            if (angleToRightIntersection > 0 && angleToRightIntersection < arcAngle) {
                // right intersection inside corner arc
                ToolpathConnection connection = new ToolpathConnection(cornerPoint);
                otherSegment.split(rightToIntersectionPoint, !fromToLeft, fromToLeft, connection);
                if (arcBegin == arcCurrent) {
                    current.setToConnection(connection);
                } else {
                    next.setFromConnection(connection);

                }
            }
        }
    }

    private void intersectWithSameSideCorners(List<ToolpathSegment> sameSideSegments,
                                              List<ToolpathSegment> allSegments) {
        int nSameSideSegments = sameSideSegments.size();
        for (int i = 0; i < nSameSideSegments; ++i) {
            ToolpathSegment current = sameSideSegments.get(i);
            ToolpathSegment next = sameSideSegments.get((i + 1) % nSameSideSegments);
            intersectWithCorner(current, next, allSegments);

        }
    }

    private static List<ToolpathSegment> getAllValidSegments(List<ToolpathSegment> toolpathSegments) {
        return toolpathSegments.stream()
                .flatMap(toolpathSegment -> toolpathSegment.getValidSegments().stream())
                .collect(Collectors.toList());
    }

    private static boolean isSamePoint(Point2D p1, Point2D p2) {
        if (p1 == null || p2 == null) {
            return false;
        }
        if (p1.equals(p2)) {
            return true;
        }
        return p1.distance(p2) < MIN_POINT_DISTANCE;
    }

    private static boolean isConnected(ToolpathSegment prev, ToolpathSegment next) {
        if (prev.getToConnection().equals(next.getFromConnection())) {
            return true;
        }
        return isSamePoint(prev.getToConnection().getConnectionPoint(),
                next.getFromConnection().getConnectionPoint());
    }

    private static boolean isToolpathClosed(List<ToolpathSegment> toolpath) {
        if (toolpath.size() < 3) {
            return false;
        }
        ToolpathSegment first = toolpath.get(0);
        ToolpathSegment last = toolpath.get(toolpath.size() - 1);
        return isConnected(last, first);
    }

    private static boolean closeToolpath(List<ToolpathSegment> toolpath) {
        for (int firstIndex = 0; firstIndex < toolpath.size() - 2; ++firstIndex) {
            for (int lastIndex = toolpath.size() - 1; lastIndex > firstIndex + 1; --lastIndex) {
                if (isConnected(toolpath.get(lastIndex), toolpath.get(firstIndex))) {
                    if (lastIndex < toolpath.size() - 1) {
                        toolpath.subList(lastIndex + 1, toolpath.size()).clear();
                    }
                    if (firstIndex > 0) {
                        toolpath.subList(0, firstIndex).clear();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private static List<List<ToolpathSegment>> partitionToolpaths(List<ToolpathSegment> validSegments) {
        List<List<ToolpathSegment>> result = new ArrayList<>();
        List<ToolpathSegment> currentToolpath = new ArrayList<>();
        LinkedList<ToolpathSegment> remainingSegments = new LinkedList<>(validSegments);
        ToolpathSegment current = remainingSegments.pollFirst();
        while (current != null) {
            currentToolpath.add(current);

            ToolpathSegment next = null;

            ListIterator<ToolpathSegment> segmentIterator = remainingSegments.listIterator();
            while (segmentIterator.hasNext()) {
                ToolpathSegment other = segmentIterator.next();
                if (current.getToConnection().equals(other.getFromConnection())) {
                    next = other;
                    segmentIterator.remove();
                    break;
                }
                if (current.getToConnection().equals(other.getToConnection())) {
                    next = other.flip();
                    segmentIterator.remove();
                    break;
                }
            }

            if (next == null) {
                segmentIterator = remainingSegments.listIterator();
                while (segmentIterator.hasNext()) {
                    ToolpathSegment other = segmentIterator.next();
                    if (isSamePoint(current.getToConnection().getConnectionPoint(),
                            other.getFromConnection().getConnectionPoint())) {
                        next = other;
                        segmentIterator.remove();
                        break;
                    }
                    if (isSamePoint(current.getToConnection().getConnectionPoint(),
                            other.getToConnection().getConnectionPoint())) {
                        next = other.flip();
                        segmentIterator.remove();
                        break;
                    }
                }
            }

            if (next != null) {
                // next found; continue adding to current toolpath
                current = next;
            } else {
                // next not found; finish current toolpath
                if (closeToolpath(currentToolpath)) {
                    result.add(currentToolpath);
                }
                currentToolpath = new ArrayList<>();
                current = remainingSegments.pollFirst();
            }
        }
        return result;
    }

    private boolean isInsideSegment(List<Segment> path, ToolpathSegment segment) {
        return Segment.isPointInsidePath(path, segment.getFrom()) && Segment.isPointInsidePath(path, segment.getTo());
    }

    private boolean isOutsideSegment(List<Segment> path, ToolpathSegment segment) {
        return !Segment.isPointInsidePath(path, segment.getFrom()) && !Segment.isPointInsidePath(path, segment.getTo());
    }

    private static void drawCircle(GraphicsContext ctx, Point2D center, double radius) {
        ctx.fillOval(center.getX() - radius, center.getY() - radius, radius*2, radius*2);
    }

    private void drawPoint(GraphicsContext ctx, Point2D point) {
        drawCircle(ctx, point, pointRadius);
    }

    private static void drawLine(GraphicsContext ctx, Point2D fromPoint, Point2D toPoint) {
        ctx.strokeLine(fromPoint.getX(), fromPoint.getY(),
                toPoint.getX(), toPoint.getY());
    }

    private static void drawLine(GraphicsContext ctx, Segment segment) {
        drawLine(ctx, segment.getFrom(), segment.getTo());
    }

    private void drawArc(GraphicsContext ctx, Point2D center, Point2D start, Point2D end) {
        Segment centerToStart = Segment.of(center, start);
        drawLine(ctx, centerToStart);
        Segment centerToEnd = Segment.of(center, end);
        drawLine(ctx, centerToEnd);
        double radius = centerToStart.getLength();
        double angleToStart = centerToStart.getAngle();
        double angleToEnd = centerToEnd.getAngle();
        double cornerAngle = Math2D.subtractAngle(angleToStart, angleToEnd);
        if (cornerAngle < 0) {
            cornerAngle = -cornerAngle;
            angleToStart = angleToEnd;
        }
        ctx.strokeArc(center.getX() - radius, center.getY() - radius, radius*2, radius*2,
                360 - Math2D.convertToDegrees(angleToStart),
                Math2D.convertToDegrees(cornerAngle),
                ArcType.OPEN);
    }

    private static void setValidStroke(GraphicsContext ctx, boolean valid) {
        if (valid) {
            ctx.setStroke(VALID_PAINT);
        } else {
            ctx.setStroke(INVALID_PAINT);
        }
    }

    private void drawSplitPoints(GraphicsContext ctx, ToolpathSegment toolpathSegment) {
        toolpathSegment.sortSplitPoints();
        ctx.save();
        Point2D lastSplitPoint = toolpathSegment.getFrom();
        boolean toSideValid = true;
        for (ToolpathSplitPoint splitPoint : toolpathSegment.getSplitPoints()) {
            setValidStroke(ctx, splitPoint.isFromSideValid());
            drawLine(ctx, lastSplitPoint, splitPoint.getPoint());
            lastSplitPoint = splitPoint.getPoint();
            toSideValid = splitPoint.isToSideValid();
        }
        setValidStroke(ctx, toSideValid);
        drawLine(ctx, lastSplitPoint, toolpathSegment.getTo());
        ctx.setStroke(PATH_PAINT);
        for (ToolpathSplitPoint splitPoint : toolpathSegment.getSplitPoints()) {
            drawPoint(ctx, splitPoint.getPoint());
        }
        ctx.restore();
    }

    private void drawToolpathSegment(GraphicsContext ctx, ToolpathSegment toolpathSegment) {
        drawLine(ctx, toolpathSegment.getSegment());
    }

    private void drawValidSegment(GraphicsContext ctx, ToolpathSegment toolpathSegment) {
        ctx.setStroke(VALID_PAINT);
        drawToolpathSegment(ctx, toolpathSegment);
        Point2D fromConnectionPoint = toolpathSegment.getFromConnection().getConnectionPoint();
        if (fromConnectionPoint != null) {
            ctx.setStroke(FROM_PAINT);
            drawLine(ctx, toolpathSegment.getFrom(), fromConnectionPoint);
        }
        Point2D toConnectionPoint = toolpathSegment.getToConnection().getConnectionPoint();
        if (toConnectionPoint != null) {
            ctx.setStroke(TO_PAINT);
            drawLine(ctx, toolpathSegment.getTo(), toConnectionPoint);
        }
    }

    private void drawPartitionedToolpaths(GraphicsContext ctx, List<List<ToolpathSegment>> toolpaths) {
        int toolpathIndex = 0;
        for (List<ToolpathSegment> toolpath : toolpaths) {
            ++toolpathIndex;

            boolean closed = isToolpathClosed(toolpath);

            ToolpathSegment prevSegment = toolpath.get(toolpath.size() - 1);
            int segmentIndex = 0;
            for (ToolpathSegment segment : toolpath) {
                ++segmentIndex;

                Point2D textPoint = segment.getFrom().midpoint(segment.getTo());
                ctx.strokeText(String.format("%d.%d", toolpathIndex, segmentIndex),
                        textPoint.getX(), textPoint.getY());

                if (!closed) {
                    ctx.setLineDashes(pointRadius, pointRadius);
                }
                Point2D connectionPoint = segment.getFromConnection().getConnectionPoint();
                if (!isSamePoint(segment.getFrom(), connectionPoint)) {
                    // draw an arc to join outside corner segments
                    drawArc(ctx, connectionPoint, prevSegment.getTo(), segment.getFrom());
                }
                drawToolpathSegment(ctx, segment);
                ctx.setLineDashes();

                prevSegment = segment;
            }

        }
    }

    public enum DisplayMode {
        SPLIT_POINTS,
        VALID_SEGMENTS,
        PARTITIONED_TOOLPATHS,
        INSIDE_OUTSIDE
    }

    public void drawToolpath(GraphicsContext ctx, DisplayMode displayMode) {
        List<Segment> connectedEdges = new ArrayList<>();
        List<List<ToolpathSegment>> connectedToolpathSides = new ArrayList<>();

        for (Path path : paths) {
            if (path.isClosed()) {
                List<ToolpathSegment> leftToolpathSegments = new ArrayList<>();
                List<ToolpathSegment> rightToolpathSegments = new ArrayList<>();
                for (Segment edge : path.getSegments()) {
                    drawPoint(ctx, edge.getFrom());
                    drawLine(ctx, edge);

                    connectedEdges.add(edge);

                    ToolpathSegment[] toolpathSegments = computeToolpathSegments(edge, toolRadius);
                    leftToolpathSegments.add(toolpathSegments[0]);
                    rightToolpathSegments.add(toolpathSegments[1]);
                }
                connectToolpathSegments(leftToolpathSegments);
                connectedToolpathSides.add(leftToolpathSegments);
                connectToolpathSegments(rightToolpathSegments);
                connectedToolpathSides.add(rightToolpathSegments);
            } else {
                Point prevPoint = null;
                for (Point point : path.getPoints()) {
                    drawPoint(ctx, point.asPoint2D());
                    if (prevPoint != null) {
                        drawLine(ctx, prevPoint.asPoint2D(), point.asPoint2D());
                    }
                    prevPoint = point;
                }
            }
        }

        if (!connectedToolpathSides.isEmpty()) {
            List<ToolpathSegment> allToolpathSegments = new ArrayList<>();
            connectedToolpathSides.forEach(allToolpathSegments::addAll);
            for (List<ToolpathSegment> sameSideSegments : connectedToolpathSides) {
                intersectWithSameSideCorners(sameSideSegments, allToolpathSegments);
            }
            for (int i = 0; i < allToolpathSegments.size(); ++i) {
                intersectToolpathSegments(i, allToolpathSegments);
            }
            if (displayMode == DisplayMode.SPLIT_POINTS) {
                allToolpathSegments.forEach(segment -> drawSplitPoints(ctx, segment));
            } else {
                List<ToolpathSegment> allValidSegments = getAllValidSegments(allToolpathSegments);
                if (displayMode == DisplayMode.VALID_SEGMENTS) {
                    ctx.save();
                    ctx.setStroke(VALID_PAINT);
                    allValidSegments.forEach(segment -> drawValidSegment(ctx, segment));
                    ctx.restore();
                } else {
                    List<ToolpathSegment> insideSegments = allValidSegments.stream()
                            .filter(segment -> isInsideSegment(connectedEdges, segment)).collect(Collectors.toList());
                    List<ToolpathSegment> outsideSegments = allValidSegments.stream()
                            .filter(segment -> isOutsideSegment(connectedEdges, segment)).collect(Collectors.toList());
                    if (displayMode == DisplayMode.INSIDE_OUTSIDE) {
                        ctx.save();
                        ctx.setStroke(INSIDE_PAINT);
                        insideSegments.forEach(segment -> drawToolpathSegment(ctx, segment));
                        ctx.setStroke(OUTSIDE_PAINT);
                        outsideSegments.forEach(segment -> drawToolpathSegment(ctx, segment));
                        ctx.restore();
                    } else {
                        List<List<ToolpathSegment>> insideToolpaths = partitionToolpaths(insideSegments);
                        List<List<ToolpathSegment>> outsideToolpaths = partitionToolpaths(outsideSegments);
                        if (displayMode == DisplayMode.PARTITIONED_TOOLPATHS) {
                            ctx.save();
                            ctx.setStroke(INSIDE_PAINT);
                            drawPartitionedToolpaths(ctx, insideToolpaths);
                            ctx.setStroke(OUTSIDE_PAINT);
                            drawPartitionedToolpaths(ctx, outsideToolpaths);
                            ctx.restore();
                        }
                    }
                }
            }
        }
    }
}
