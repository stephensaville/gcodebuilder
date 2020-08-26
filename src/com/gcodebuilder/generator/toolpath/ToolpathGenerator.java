package com.gcodebuilder.generator.toolpath;

import com.gcodebuilder.app.tools.Tool;
import com.gcodebuilder.geometry.Math2D;
import com.gcodebuilder.geometry.Path;
import com.gcodebuilder.geometry.Point;
import com.gcodebuilder.geometry.Segment;
import com.gcodebuilder.geometry.UnitVector;
import com.gcodebuilder.model.Direction;
import com.gcodebuilder.model.Side;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;
import javafx.scene.transform.Affine;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.stream.Collectors;

public class ToolpathGenerator {
    private static final Logger log = LogManager.getLogger(ToolpathGenerator.class);

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

    @Getter @Setter
    private double stepOver = 0.4;

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

    private Toolpath.Segment[] computeToolpathSegments(Segment edge, double toolRadius) {
        UnitVector left = edge.getDirection().leftNormal();
        UnitVector right = edge.getDirection().rightNormal();

        return new Toolpath.Segment[] {
                Toolpath.Segment.fromEdge(edge, toolRadius, right, left),
                Toolpath.Segment.fromEdge(edge, toolRadius, left, right)
        };
    }

    private void connectToolpathSegments(List<Toolpath.Segment> sameSideSegments) {
        Toolpath.Segment prev = sameSideSegments.get(sameSideSegments.size() - 1);
        for (Toolpath.Segment current : sameSideSegments) {
            prev.setToConnection(current.getFromConnection());
            prev = current;
        }
    }

    private void intersectToolpathSegments(int i, List<Toolpath.Segment> segments) {
        Toolpath.Segment current = segments.get(i);
        for (int j = i + 1; j < segments.size(); ++j) {
            Toolpath.Segment other = segments.get(j);
            Point2D intersectionPoint = current.intersect(other);
            if (intersectionPoint != null) {
                Toolpath.Connection connection = new Toolpath.Connection(intersectionPoint);

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

    private void intersectWithCorner(Toolpath.Segment current, Toolpath.Segment next,
                                     List<Toolpath.Segment> segments) {
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

        for (Toolpath.Segment otherSegment : segments) {
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
                Toolpath.Connection connection = new Toolpath.Connection(cornerPoint);
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
                Toolpath.Connection connection = new Toolpath.Connection(cornerPoint);
                otherSegment.split(rightToIntersectionPoint, !fromToLeft, fromToLeft, connection);
                if (arcBegin == arcCurrent) {
                    current.setToConnection(connection);
                } else {
                    next.setFromConnection(connection);

                }
            }
        }
    }

    private void intersectWithSameSideCorners(List<Toolpath.Segment> sameSideSegments,
                                              List<Toolpath.Segment> allSegments) {
        int nSameSideSegments = sameSideSegments.size();
        for (int i = 0; i < nSameSideSegments; ++i) {
            Toolpath.Segment current = sameSideSegments.get(i);
            Toolpath.Segment next = sameSideSegments.get((i + 1) % nSameSideSegments);
            intersectWithCorner(current, next, allSegments);

        }
    }

    private static List<Toolpath.Segment> getAllValidSegments(List<Toolpath.Segment> segments) {
        return segments.stream()
                .flatMap(segment -> segment.getValidSegments().stream())
                .collect(Collectors.toList());
    }

    public static boolean isSamePoint(Point2D p1, Point2D p2) {
        if (p1 == null || p2 == null) {
            return false;
        }
        if (p1.equals(p2)) {
            return true;
        }
        return p1.distance(p2) < MIN_POINT_DISTANCE;
    }

    private static boolean isConnected(Toolpath.Segment prev, Toolpath.Segment next) {
        if (prev.getToConnection().equals(next.getFromConnection())) {
            return true;
        }
        return isSamePoint(prev.getToConnection().getConnectionPoint(),
                next.getFromConnection().getConnectionPoint());
    }

    private static boolean isToolpathClosed(List<Toolpath.Segment> toolpath) {
        if (toolpath.size() < 3) {
            return false;
        }
        Toolpath.Segment first = toolpath.get(0);
        Toolpath.Segment last = toolpath.get(toolpath.size() - 1);
        return isConnected(last, first);
    }

    private static boolean closeToolpath(List<Toolpath.Segment> toolpath) {
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

    private static List<List<Toolpath.Segment>> partitionToolpaths(List<Toolpath.Segment> validSegments) {
        List<List<Toolpath.Segment>> result = new ArrayList<>();
        List<Toolpath.Segment> currentToolpath = new ArrayList<>();
        LinkedList<Toolpath.Segment> remainingSegments = new LinkedList<>(validSegments);
        Toolpath.Segment current = remainingSegments.pollFirst();
        while (current != null) {
            currentToolpath.add(current);

            Toolpath.Segment next = null;

            ListIterator<Toolpath.Segment> segmentIterator = remainingSegments.listIterator();
            while (segmentIterator.hasNext()) {
                Toolpath.Segment other = segmentIterator.next();
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
                    Toolpath.Segment other = segmentIterator.next();
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

    private boolean isInsideSegment(List<Segment> path, Toolpath.Segment segment) {
        return Segment.isPointInsidePath(path, segment.getFrom()) && Segment.isPointInsidePath(path, segment.getTo());
    }

    private boolean isOutsideSegment(List<Segment> path, Toolpath.Segment segment) {
        return !Segment.isPointInsidePath(path, segment.getFrom()) && !Segment.isPointInsidePath(path, segment.getTo());
    }

    private Direction getToolpathDirection(List<Toolpath.Segment> toolpath) {
        double totalAngleDiff = 0.0;
        Toolpath.Segment prevSegment = toolpath.get(toolpath.size() - 1);
        for (Toolpath.Segment segment : toolpath) {
            totalAngleDiff += Math2D.subtractAngle(segment.getSegment().getAngle(),
                    prevSegment.getSegment().getAngle());
            prevSegment = segment;
        }
        return (totalAngleDiff > 0) ? Direction.COUNTER_CLOCKWISE : Direction.CLOCKWISE;
    }

    private List<Toolpath.Segment> reverseToolpath(List<Toolpath.Segment> toolpath) {
        List<Toolpath.Segment> reversed = new ArrayList<>(toolpath.size());
        for (int i = toolpath.size() - 1; i >= 0; --i) {
            reversed.add(toolpath.get(i).flip());
        }
        return reversed;
    }

    private List<Toolpath.Segment> orientToolpath(List<Toolpath.Segment> toolpath, Direction direction) {
        if (getToolpathDirection(toolpath) == direction) {
            return toolpath;
        } else {
            return reverseToolpath(toolpath);
        }
    }

    public List<Toolpath> computeToolpaths(Side side, Direction direction) {
        List<Segment> connectedEdges = new ArrayList<>();
        List<List<Toolpath.Segment>> connectedToolpathSides = new ArrayList<>();

        for (Path path : paths) {
            if (path.isClosed()) {
                List<Toolpath.Segment> leftSegments = new ArrayList<>();
                List<Toolpath.Segment> rightSegments = new ArrayList<>();
                for (Segment edge : path.getSegments()) {
                    connectedEdges.add(edge);

                    Toolpath.Segment[] segments = computeToolpathSegments(edge, toolRadius);
                    leftSegments.add(segments[0]);
                    rightSegments.add(segments[1]);
                }
                connectToolpathSegments(leftSegments);
                connectedToolpathSides.add(leftSegments);
                connectToolpathSegments(rightSegments);
                connectedToolpathSides.add(rightSegments);
            }
        }

        if (!connectedToolpathSides.isEmpty()) {
            List<Toolpath.Segment> allSegments = new ArrayList<>();
            connectedToolpathSides.forEach(allSegments::addAll);
            for (List<Toolpath.Segment> sameSideSegments : connectedToolpathSides) {
                intersectWithSameSideCorners(sameSideSegments, allSegments);
            }
            for (int i = 0; i < allSegments.size(); ++i) {
                intersectToolpathSegments(i, allSegments);
            }
            List<Toolpath.Segment> allValidSegments = getAllValidSegments(allSegments);
            List<Toolpath.Segment> sideSegments;
            switch (side) {
            case INSIDE:
                sideSegments = allValidSegments.stream()
                        .filter(segment -> isInsideSegment(connectedEdges, segment))
                        .collect(Collectors.toList());
                break;
            case OUTSIDE:
                sideSegments = allValidSegments.stream()
                        .filter(segment -> isOutsideSegment(connectedEdges, segment))
                        .collect(Collectors.toList());
                break;
            default:
                sideSegments = Collections.emptyList();
                break;
            }
            List<List<Toolpath.Segment>> partitionedToolpaths = partitionToolpaths(sideSegments);
            return partitionedToolpaths.stream()
                    .map(toolpath -> orientToolpath(toolpath, direction))
                    .map(Toolpath::new)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private List<Segment> computeEnclosingPath(List<Toolpath.Segment> enclosingToolpath) {
        List<Segment> enclosingPath = new ArrayList<>(enclosingToolpath.size());
        for (int i = 0; i < enclosingToolpath.size(); ++i) {
            Toolpath.Segment current = enclosingToolpath.get(i);

            Point2D from = current.getFrom();
            if (!isSamePoint(from, current.getFromConnection().getConnectionPoint())) {
                int prevIndex = (i > 0) ? (i - 1) : (enclosingToolpath.size() - 1);
                Toolpath.Segment prev = enclosingToolpath.get(prevIndex);
                from = current.getSegment().intersect(prev.getSegment(), true);
            }

            Point2D to = current.getTo();
            if (!isSamePoint(to, current.getToConnection().getConnectionPoint())) {
                int nextIndex = (i + 1 < enclosingToolpath.size()) ? (i + 1) : 0;
                Toolpath.Segment next = enclosingToolpath.get(nextIndex);
                to = current.getSegment().intersect(next.getSegment(), true);
            }

            enclosingPath.add(Segment.of(from, to));
        }
        return enclosingPath;
    }

    private List<Toolpath.Segment> computePocketSegments(List<Toolpath.Segment> enclosingToolpath,
                                                         List<Segment> enclosingPath) {
        List<Toolpath.Segment> pocketSegments = new ArrayList<>(enclosingToolpath.size());
        Iterator<Segment> pathIterator = enclosingPath.iterator();
        for (Toolpath.Segment toolpathSegment : enclosingToolpath) {
            pocketSegments.add(Toolpath.Segment.fromEdge(
                    pathIterator.next(),
                    toolRadius*stepOver*2.0,
                    toolpathSegment.getTowards(),
                    toolpathSegment.getAway()
            ));
        }
        return pocketSegments;
    }

    private List<List<Toolpath.Segment>> computePockets(List<List<Toolpath.Segment>> insideToolpaths) {
        List<Toolpath.Segment> allSegments = new ArrayList<>();
        insideToolpaths.forEach(allSegments::addAll);

        List<List<Toolpath.Segment>> pocketToolpaths = new ArrayList<>();
        pocketToolpaths.addAll(insideToolpaths);

        Queue<List<Toolpath.Segment>> enclosingToolpathQueue = new LinkedList<>();
        enclosingToolpathQueue.addAll(insideToolpaths);

        final int maxIterationCount = insideToolpaths.size() * 10;
        int iterationCount = 0;

        while (!enclosingToolpathQueue.isEmpty()) {
            if (maxIterationCount <= iterationCount++) {
                log.info("Reached max iterations!");
                break;
            } else {
                log.info("Iteration #{}", iterationCount);
            }

            List<Toolpath.Segment> enclosingToolpath = enclosingToolpathQueue.remove();
            List<Segment> enclosingPath = computeEnclosingPath(enclosingToolpath);
            List<Toolpath.Segment> pocketSegments = computePocketSegments(enclosingToolpath, enclosingPath);

            connectToolpathSegments(pocketSegments);

            int pocketStartIndex = allSegments.size();
            allSegments.addAll(pocketSegments);
            intersectWithSameSideCorners(pocketSegments, allSegments);
            for (int i = pocketStartIndex; i < allSegments.size(); ++i) {
                intersectToolpathSegments(i, allSegments);
            }

            List<Toolpath.Segment> validPocketSegments = getAllValidSegments(pocketSegments);

            List<Toolpath.Segment> insidePocketSegments = validPocketSegments.stream()
                    .filter(segment -> isInsideSegment(enclosingPath, segment))
                    .collect(Collectors.toList());

            List<List<Toolpath.Segment>> partitionedPocketToolpaths = partitionToolpaths(insidePocketSegments);

            pocketToolpaths.addAll(partitionedPocketToolpaths);
            enclosingToolpathQueue.addAll(partitionedPocketToolpaths);
        }
        return pocketToolpaths;
    }

    private List<List<List<Toolpath.Segment>>> connectPockets(List<List<Toolpath.Segment>> pocketToolpaths) {
        List<List<List<Toolpath.Segment>>> allConnectedPockets = new ArrayList<>();
        LinkedList<List<Toolpath.Segment>> remainingPockets = new LinkedList<>(pocketToolpaths);
        List<List<Toolpath.Segment>> currentConnectedPockets = new ArrayList<>();
        List<Toolpath.Segment> currentPocket = remainingPockets.pollFirst();
        while (currentPocket != null) {
            currentConnectedPockets.add(currentPocket);
            List<Segment> pocketPath = computeEnclosingPath(currentPocket);
            Point2D currentPoint = currentPocket.get(currentPocket.size() - 1).getTo();

            List<Toolpath.Segment> nextPocket = null;
            double nextPocketDistance = Double.MAX_VALUE;

            ListIterator<List<Toolpath.Segment>> toolpathIterator = remainingPockets.listIterator();
            while (toolpathIterator.hasNext()) {
                List<Toolpath.Segment> otherPocket = toolpathIterator.next();
                Point2D startPoint = otherPocket.get(otherPocket.size() - 1).getTo();
                if (Segment.isPointInsidePath(pocketPath, startPoint)) {
                    double pocketDistance = currentPoint.distance(startPoint);
                    if (pocketDistance < nextPocketDistance) {
                        if (nextPocket != null) {
                            toolpathIterator.set(nextPocket);
                        } else {
                            toolpathIterator.remove();
                        }
                        nextPocket = otherPocket;
                        nextPocketDistance = pocketDistance;
                    }
                }
            }

            if (nextPocket != null) {
                currentPocket = nextPocket;
            } else {
                allConnectedPockets.add(currentConnectedPockets);
                currentConnectedPockets = new ArrayList<>();
                currentPocket = remainingPockets.pollFirst();
            }
        }
        return allConnectedPockets;
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

    private void drawSplitPoints(GraphicsContext ctx, Toolpath.Segment segment) {
        segment.sortSplitPoints();
        ctx.save();
        Point2D lastSplitPoint = segment.getFrom();
        boolean toSideValid = true;
        for (Toolpath.SplitPoint splitPoint : segment.getSplitPoints()) {
            setValidStroke(ctx, splitPoint.isFromSideValid());
            drawLine(ctx, lastSplitPoint, splitPoint.getPoint());
            lastSplitPoint = splitPoint.getPoint();
            toSideValid = splitPoint.isToSideValid();
        }
        setValidStroke(ctx, toSideValid);
        drawLine(ctx, lastSplitPoint, segment.getTo());
        ctx.setStroke(PATH_PAINT);
        for (Toolpath.SplitPoint splitPoint : segment.getSplitPoints()) {
            drawPoint(ctx, splitPoint.getPoint());
        }
        ctx.restore();
    }

    private void drawToolpathSegment(GraphicsContext ctx, Toolpath.Segment segment) {
        drawLine(ctx, segment.getSegment());
    }

    private void drawValidSegment(GraphicsContext ctx, Toolpath.Segment segment) {
        ctx.setStroke(VALID_PAINT);
        drawToolpathSegment(ctx, segment);
        Point2D fromConnectionPoint = segment.getFromConnection().getConnectionPoint();
        if (fromConnectionPoint != null) {
            ctx.setStroke(FROM_PAINT);
            drawLine(ctx, segment.getFrom(), fromConnectionPoint);
        }
        Point2D toConnectionPoint = segment.getToConnection().getConnectionPoint();
        if (toConnectionPoint != null) {
            ctx.setStroke(TO_PAINT);
            drawLine(ctx, segment.getTo(), toConnectionPoint);
        }
    }

    private void drawText(GraphicsContext ctx, Point2D textPoint, String text) {
        ctx.save();
        try {
            textPoint = ctx.getTransform().transform(textPoint);
            ctx.setTransform(new Affine());
            ctx.strokeText(text, textPoint.getX(), textPoint.getY());
        } finally {
            ctx.restore();
        }
    }

    private void drawPartitionedToolpaths(GraphicsContext ctx, List<List<Toolpath.Segment>> toolpaths,
                                          boolean connected, int startingToolpathIndex) {
        int toolpathIndex = startingToolpathIndex;
        Toolpath.Segment prevToolpathSegment = null;
        for (List<Toolpath.Segment> toolpath : toolpaths) {
            ++toolpathIndex;

            boolean closed = isToolpathClosed(toolpath);

            Direction toolpathDirection = getToolpathDirection(toolpath);
            String directionText = (toolpathDirection == Direction.CLOCKWISE) ? "CW" : "CCW";

            Toolpath.Segment prevSegment = toolpath.get(toolpath.size() - 1);
            if (connected && prevToolpathSegment != null) {
                drawLine(ctx, prevToolpathSegment.getTo(), prevSegment.getTo());
            }
            prevToolpathSegment = prevSegment;

            int segmentIndex = 0;
            for (Toolpath.Segment segment : toolpath) {
                ++segmentIndex;

                Point2D textPoint = segment.getFrom().midpoint(segment.getTo());
                String text;
                if (directionText != null) {
                    text = String.format("%d.%d (%s)", toolpathIndex, segmentIndex, directionText);
                    directionText = null;
                } else {
                    text = String.format("%d.%d", toolpathIndex, segmentIndex);
                }
                drawText(ctx, textPoint, text);

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

    private void drawPartitionedToolpaths(GraphicsContext ctx, List<List<Toolpath.Segment>> toolpaths) {
        drawPartitionedToolpaths(ctx, toolpaths, false, 0);
    }

    public enum DisplayMode {
        SPLIT_POINTS,
        VALID_SEGMENTS,
        INSIDE_OUTSIDE,
        PARTITIONED_TOOLPATHS,
        ORIENTED_TOOLPATHS,
        POCKETS,
        CONNECTED_POCKETS
    }

    public void drawToolpath(GraphicsContext ctx, DisplayMode displayMode) {
        List<Segment> connectedEdges = new ArrayList<>();
        List<List<Toolpath.Segment>> connectedToolpathSides = new ArrayList<>();

        for (Path path : paths) {
            if (path.isClosed()) {
                List<Toolpath.Segment> leftSegments = new ArrayList<>();
                List<Toolpath.Segment> rightSegments = new ArrayList<>();
                for (Segment edge : path.getSegments()) {
                    drawPoint(ctx, edge.getFrom());
                    drawLine(ctx, edge);

                    connectedEdges.add(edge);

                    Toolpath.Segment[] segments = computeToolpathSegments(edge, toolRadius);
                    leftSegments.add(segments[0]);
                    rightSegments.add(segments[1]);
                }
                connectToolpathSegments(leftSegments);
                connectedToolpathSides.add(leftSegments);
                connectToolpathSegments(rightSegments);
                connectedToolpathSides.add(rightSegments);
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
            List<Toolpath.Segment> allSegments = new ArrayList<>();
            connectedToolpathSides.forEach(allSegments::addAll);
            for (List<Toolpath.Segment> sameSideSegments : connectedToolpathSides) {
                intersectWithSameSideCorners(sameSideSegments, allSegments);
            }
            for (int i = 0; i < allSegments.size(); ++i) {
                intersectToolpathSegments(i, allSegments);
            }
            if (displayMode == DisplayMode.SPLIT_POINTS) {
                allSegments.forEach(segment -> drawSplitPoints(ctx, segment));
            } else {
                List<Toolpath.Segment> allValidSegments = getAllValidSegments(allSegments);
                if (displayMode == DisplayMode.VALID_SEGMENTS) {
                    ctx.save();
                    try {
                        ctx.setStroke(VALID_PAINT);
                        allValidSegments.forEach(segment -> drawValidSegment(ctx, segment));
                    } finally {
                        ctx.restore();
                    }
                } else {
                    List<Toolpath.Segment> insideSegments = allValidSegments.stream()
                            .filter(segment -> isInsideSegment(connectedEdges, segment)).collect(Collectors.toList());
                    List<Toolpath.Segment> outsideSegments = allValidSegments.stream()
                            .filter(segment -> isOutsideSegment(connectedEdges, segment)).collect(Collectors.toList());
                    if (displayMode == DisplayMode.INSIDE_OUTSIDE) {
                        ctx.save();
                        try {
                            ctx.setStroke(INSIDE_PAINT);
                            insideSegments.forEach(segment -> drawToolpathSegment(ctx, segment));
                            ctx.setStroke(OUTSIDE_PAINT);
                            outsideSegments.forEach(segment -> drawToolpathSegment(ctx, segment));
                        } finally {
                            ctx.restore();
                        }
                    } else {
                        List<List<Toolpath.Segment>> insideToolpaths = partitionToolpaths(insideSegments);
                        List<List<Toolpath.Segment>> outsideToolpaths = partitionToolpaths(outsideSegments);
                        if (displayMode == DisplayMode.PARTITIONED_TOOLPATHS) {
                            ctx.save();
                            try {
                                ctx.setStroke(INSIDE_PAINT);
                                drawPartitionedToolpaths(ctx, insideToolpaths);
                                ctx.setStroke(OUTSIDE_PAINT);
                                drawPartitionedToolpaths(ctx, outsideToolpaths);
                            } finally {
                                ctx.restore();
                            }
                        } else {
                            insideToolpaths = insideToolpaths.stream()
                                    .map(toolpath -> orientToolpath(toolpath, Direction.CLOCKWISE))
                                    .collect(Collectors.toList());
                            outsideToolpaths = outsideToolpaths.stream()
                                    .map(toolpath -> orientToolpath(toolpath, Direction.CLOCKWISE))
                                    .collect(Collectors.toList());
                            if (displayMode == DisplayMode.ORIENTED_TOOLPATHS) {
                                ctx.save();
                                try {
                                    ctx.setStroke(INSIDE_PAINT);
                                    drawPartitionedToolpaths(ctx, insideToolpaths);
                                    ctx.setStroke(OUTSIDE_PAINT);
                                    drawPartitionedToolpaths(ctx, outsideToolpaths);
                                } finally {
                                    ctx.restore();
                                }
                            } else {
                                List<List<Toolpath.Segment>> pocketToolpaths = computePockets(insideToolpaths);
                                if (displayMode == DisplayMode.POCKETS) {
                                    ctx.save();
                                    try {
                                        ctx.setStroke(INSIDE_PAINT);
                                        drawPartitionedToolpaths(ctx, pocketToolpaths);
                                    } finally {
                                        ctx.restore();
                                    }
                                } else if (displayMode == DisplayMode.CONNECTED_POCKETS) {
                                    List<List<List<Toolpath.Segment>>> connectedPocketToolpaths =
                                            connectPockets(pocketToolpaths);
                                    ctx.save();
                                    try {
                                        ctx.setStroke(INSIDE_PAINT);
                                        int startingToolpathIndex = 0;
                                        for (List<List<Toolpath.Segment>> connectedPockets : connectedPocketToolpaths) {
                                            drawPartitionedToolpaths(ctx, connectedPockets, true, startingToolpathIndex);
                                            startingToolpathIndex += connectedPockets.size();
                                        }
                                    } finally {
                                        ctx.restore();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
