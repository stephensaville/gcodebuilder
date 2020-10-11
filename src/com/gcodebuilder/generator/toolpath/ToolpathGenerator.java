package com.gcodebuilder.generator.toolpath;

import com.gcodebuilder.geometry.ArcSegment;
import com.gcodebuilder.geometry.Math2D;
import com.gcodebuilder.geometry.Path;
import com.gcodebuilder.geometry.LineSegment;
import com.gcodebuilder.geometry.PathSegment;
import com.gcodebuilder.geometry.UnitVector;
import com.gcodebuilder.model.Direction;
import com.gcodebuilder.model.Side;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ToolpathGenerator {
    private static final Logger log = LogManager.getLogger(ToolpathGenerator.class);

    private static final Paint PATH_PAINT = Color.BLACK;
    private static final Paint SEGMENT_PAINT = Color.GRAY;
    private static final Paint VALID_PAINT = Color.GREEN;
    private static final Paint INVALID_PAINT = Color.RED;
    private static final Paint INSIDE_PAINT = Color.PURPLE;
    private static final Paint OUTSIDE_PAINT = Color.DARKORANGE;
    private static final Paint FROM_PAINT = Color.CYAN;
    private static final Paint TO_PAINT = Color.MAGENTA;

    @Getter
    @RequiredArgsConstructor
    public enum DisplayMode {
        CONNECTED_SEGMENTS("Connected Segments"),
        SPLIT_POINTS("Split Points"),
        VALID_SEGMENTS("Valid Segments"),
        INSIDE_OUTSIDE("Inside/Outside"),
        TOOLPATHS("Profile Toolpaths"),
        ORIENTED_TOOLPATHS("Oriented Toolpaths"),
        POCKET_CONNECTED_SEGMENTS("Pocket Connected Segments"),
        POCKET_SPLIT_POINTS("Pocket Split Points"),
        POCKET_VALID_SEGMENTS("Pocket Valid Segments"),
        POCKET_INSIDE_OUTSIDE("Pocket Inside/Outside"),
        POCKET_TOOLPATHS("Pocket Toolpaths"),
        CONNECTED_TOOLPATHS("Connected Toolpaths");

        private final String label;
    }

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

    public void addAllPaths(Collection<Path> paths) {
        this.paths.addAll(paths);
    }

    public boolean removePath(Path path) {
        return paths.remove(path);
    }

    public void clearPaths() {
        paths.clear();
    }

    private List<Toolpath.Segment> connectToolpathSegments(List<Toolpath.Segment> sameSideSegments) {
        List<Toolpath.Segment> connectedSegments = new ArrayList<>();
        Toolpath.Segment prev = sameSideSegments.get(sameSideSegments.size() - 1);
        for (Toolpath.Segment current : sameSideSegments) {
            if (Math2D.samePoints(prev.getTo(), current.getFrom())) {
                Toolpath.Connection connection = new Toolpath.Connection(current.getFrom());
                current.setFromConnection(connection);
                prev.setToConnection(connection);
            } else {
                List<Point2D> intersectionPoints = current.intersect(prev).stream()
                        .filter(PathSegment.IntersectionPoint::isOnSegments)
                        .map(PathSegment.IntersectionPoint::getPoint)
                        .collect(Collectors.toList());

                if (intersectionPoints.isEmpty()) {
                    Point2D center = current.getFromConnection().getConnectionPoint();
                    UnitVector centerToPrev = UnitVector.from(center, prev.getTo());
                    UnitVector prevDirection = prev.getSegment().getToDirection();
                    boolean clockwise = centerToPrev.rightNormal().isSame(prevDirection);

                    ArcSegment between = ArcSegment.of(prev.getTo(), center, current.getFrom(), clockwise);

                    Toolpath.Connection prevConnection = new Toolpath.Connection(between.getFrom());
                    prev.setToConnection(prevConnection);

                    Toolpath.Connection currentConnection = new Toolpath.Connection(between.getTo());
                    current.setFromConnection(currentConnection);

                    Toolpath.Segment segmentBetween = new Toolpath.Segment(between, toolRadius, current.isLeftSide(),
                            prevConnection, currentConnection);
                    connectedSegments.add(segmentBetween);
                } else {
                    Point2D intersectionPoint = intersectionPoints.stream()
                            .sorted(Math2D.distanceComparator(prev.getTo())).findFirst().get();
                    Toolpath.Connection connection = new Toolpath.Connection(intersectionPoint);
                    prev.split(intersectionPoint, true, false, connection);
                    current.split(intersectionPoint, false, true, connection);
                }
            }

            connectedSegments.add(current);
            prev = current;
        }

        return connectedSegments.stream()
                .flatMap(segment -> segment.getValidSegments().stream())
                .collect(Collectors.toList());
    }

    private static boolean isFromSideValid(Toolpath.Segment other, Point2D intersectionPoint, Toolpath.Segment current) {
        UnitVector otherSegmentDirection = other.getSegment().getDirectionAtPoint(intersectionPoint);
        UnitVector otherTowards = other.isLeftSide()
                ? otherSegmentDirection.rightNormal()
                : otherSegmentDirection.leftNormal();
        double otherTowardsAngle = otherTowards.getAngle();

        double currentSegmentAngle = current.getSegment().getAngleAtPoint(intersectionPoint);

        return Math.abs(Math2D.subtractAngle(otherTowardsAngle, currentSegmentAngle)) <= Math.PI/2;
    }

    private static Stream<Toolpath.Connection> getConnections(Toolpath.Segment current, Toolpath.Segment other) {
        Stream.Builder<Toolpath.Connection> connectionsBuilder = Stream.builder();
        if (current.getFromConnection().equals(other.getToConnection())) {
            connectionsBuilder.add(current.getFromConnection());
        }
        if (current.getToConnection().equals(other.getFromConnection())) {
            connectionsBuilder.add(current.getToConnection());
        }
        return connectionsBuilder.build();
    }

    private static Stream<Point2D> getConnectionPoints(Toolpath.Segment current, Toolpath.Segment other) {
        return getConnections(current, other).map(Toolpath.Connection::getConnectionPoint);
    }

    private static boolean isConnectionPoint(Toolpath.Segment current, Toolpath.Segment other, Point2D point) {
        return getConnectionPoints(current, other)
                .filter(connectionPoint -> Math2D.samePoints(point, connectionPoint))
                .findFirst().isPresent();
    }

    private void intersectToolpathSegments(Toolpath.Segment current, List<Toolpath.Segment> others) {
        for (Toolpath.Segment other : others) {
            List<PathSegment.IntersectionPoint> intersectionPoints = current.intersect(other);
            for (PathSegment.IntersectionPoint intersection : intersectionPoints) {
                if (intersection.isOnSegments()) {
                    Point2D intersectionPoint = intersection.getPoint();
                    if (!isConnectionPoint(current, other, intersectionPoint)) {
                        Toolpath.Connection connection = new Toolpath.Connection(intersectionPoint);

                        boolean currentFromSideValid = isFromSideValid(other, intersectionPoint, current);
                        current.split(intersectionPoint, currentFromSideValid, !currentFromSideValid, connection);

                        boolean otherFromSideValid = isFromSideValid(current, intersectionPoint, other);
                        other.split(intersectionPoint, otherFromSideValid, !otherFromSideValid, connection);
                    }
                }
            }
        }
    }

    private void intersectAllToolpathSegments(List<Toolpath.Segment> allSegments) {
        for (int i = 0; i < allSegments.size(); ++i) {
            intersectToolpathSegments(allSegments.get(i), allSegments.subList(i + 1, allSegments.size()));
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
        if (toolpath.size() < 1) {
            return false;
        }
        Toolpath.Segment first = toolpath.get(0);
        Toolpath.Segment last = toolpath.get(toolpath.size() - 1);
        return isConnected(last, first);
    }

    private static boolean closeToolpath(List<Toolpath.Segment> toolpath) {
        for (int firstIndex = 0; firstIndex < toolpath.size(); ++firstIndex) {
            for (int lastIndex = toolpath.size() - 1; lastIndex >= firstIndex; --lastIndex) {
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

    private static List<Toolpath> partitionToolpaths(List<Toolpath.Segment> validSegments) {
        List<Toolpath> result = new ArrayList<>();
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
                    result.add(new Toolpath(currentToolpath));
                }
                currentToolpath = new ArrayList<>();
                current = remainingSegments.pollFirst();
            }
        }
        return result;
    }

    private boolean isInsideSegment(List<PathSegment> path, Toolpath.Segment segment) {
        return PathSegment.isPointInsidePath(path, segment.getFrom()) && PathSegment.isPointInsidePath(path, segment.getTo());
    }

    private boolean isOutsideSegment(List<PathSegment> path, Toolpath.Segment segment) {
        return !PathSegment.isPointInsidePath(path, segment.getFrom()) && !PathSegment.isPointInsidePath(path, segment.getTo());
    }

    private List<List<Toolpath.Segment>> computeConnectedToolpathSides(List<PathSegment> connectedEdges) {
        List<List<Toolpath.Segment>> connectedToolpathSides = new ArrayList<>();
        for (Path path : paths) {
            if (path.isClosed()) {
                List<Toolpath.Segment> leftSegments = new ArrayList<>();
                List<Toolpath.Segment> rightSegments = new ArrayList<>();
                for (PathSegment edge : path.getSegments()) {
                    connectedEdges.add(edge);

                    Toolpath.SegmentPair segmentPair = edge.computeToolpathSegments(toolRadius);
                    leftSegments.add(segmentPair.getLeft());
                    rightSegments.add(segmentPair.getRight());
                }
                connectedToolpathSides.add(connectToolpathSegments(leftSegments));
                connectedToolpathSides.add(connectToolpathSegments(rightSegments));
            }
        }
        return connectedToolpathSides;
    }

    public List<Toolpath> computeProfileToolpaths(Side side, Direction direction, GraphicsContext ctx,
                                                  DisplayMode displayMode) {
        List<PathSegment> connectedEdges = new ArrayList<>();
        List<List<Toolpath.Segment>> connectedToolpathSides = computeConnectedToolpathSides(connectedEdges);

        if (ctx != null && displayMode == DisplayMode.CONNECTED_SEGMENTS) {
            ctx.setStroke(SEGMENT_PAINT);
            for (List<Toolpath.Segment> toolpathSide : connectedToolpathSides) {
                toolpathSide.forEach(segment -> drawToolpathSegment(ctx, segment));
            }
        }

        if (!connectedToolpathSides.isEmpty()) {
            List<Toolpath.Segment> allSegments = new ArrayList<>();
            connectedToolpathSides.forEach(allSegments::addAll);
            intersectAllToolpathSegments(allSegments);

            if (ctx != null && displayMode == DisplayMode.SPLIT_POINTS) {
                allSegments.forEach(segment -> drawSplitPoints(ctx, segment));
            }

            List<Toolpath.Segment> allValidSegments = getAllValidSegments(allSegments);

            if (ctx != null && displayMode == DisplayMode.VALID_SEGMENTS) {
                allValidSegments.forEach(segment -> drawValidSegment(ctx, segment));
            }

            List<Toolpath.Segment> insideSegments = allValidSegments.stream()
                    .filter(segment -> isInsideSegment(connectedEdges, segment))
                    .collect(Collectors.toList());
            List<Toolpath.Segment> outsideSegments = allValidSegments.stream()
                    .filter(segment -> isOutsideSegment(connectedEdges, segment))
                    .collect(Collectors.toList());

            if (ctx != null && displayMode == DisplayMode.INSIDE_OUTSIDE) {
                drawInsideOutsideSegments(ctx, connectedEdges, allValidSegments, insideSegments, outsideSegments);
            }

            List<Toolpath.Segment> sideSegments;
            switch (side) {
                case INSIDE:
                    sideSegments = insideSegments;
                    break;
                case OUTSIDE:
                    sideSegments = outsideSegments;
                    break;
                default:
                    sideSegments = Collections.emptyList();
                    break;
            }

            List<Toolpath> partitionedToolpaths = partitionToolpaths(sideSegments);

            if (ctx != null && displayMode == DisplayMode.TOOLPATHS) {
                drawToolpaths(ctx, partitionedToolpaths);
            }

            List<Toolpath> orientedToolpaths = partitionedToolpaths.stream()
                .map(toolpath -> toolpath.orient(direction))
                .collect(Collectors.toList());

            if (ctx != null && displayMode.compareTo(DisplayMode.ORIENTED_TOOLPATHS) >= 0) {
                drawToolpaths(ctx, orientedToolpaths);
            }

            return orientedToolpaths;
        }

        return Collections.emptyList();
    }

    public List<Toolpath> computeProfileToolpaths(Side side, Direction direction) {
        return computeProfileToolpaths(side, direction, null, null);
    }

    private static List<PathSegment> computeEnclosingPath(Toolpath enclosingToolpath) {
        return enclosingToolpath.getSegments().stream()
                .map(Toolpath.Segment::getSegment)
                .collect(Collectors.toList());
    }

    private List<Toolpath.Segment> computePocketSegments(Toolpath enclosingToolpath) {
        List<Toolpath.Segment> enclosingToolpathSegments = enclosingToolpath.getSegments();
        List<Toolpath.Segment> pocketSegments = new ArrayList<>(enclosingToolpathSegments.size());
        for (Toolpath.Segment toolpathSegment : enclosingToolpathSegments) {
            pocketSegments.add(toolpathSegment.getSegment().computeToolpathSegment(
                    toolRadius*stepOver*2.0,
                    toolpathSegment.isLeftSide()));
        }
        return connectToolpathSegments(pocketSegments);
    }

    private List<Toolpath> computePockets(List<Toolpath> insideToolpaths, GraphicsContext ctx, DisplayMode displayMode) {

        List<Toolpath.Segment> allSegments = new ArrayList<>();
        insideToolpaths.forEach(toolpath -> allSegments.addAll(toolpath.getSegments()));

        List<Toolpath> pocketToolpaths = new ArrayList<>(insideToolpaths);
        List<Toolpath> enclosingLayer = insideToolpaths;

        while (!enclosingLayer.isEmpty()) {
            List<PathSegment> connectedPath = new ArrayList<>();
            List<Toolpath.Segment> layerSegments = new ArrayList<>();
            List<List<Toolpath.Segment>> connectedLayerSegments = new ArrayList<>();

            for (Toolpath toolpath : enclosingLayer) {
                List<PathSegment> enclosingPath = computeEnclosingPath(toolpath);
                connectedPath.addAll(enclosingPath);

                List<Toolpath.Segment> pocketSegments = computePocketSegments(toolpath);

                if (ctx != null && displayMode == DisplayMode.POCKET_CONNECTED_SEGMENTS) {
                    ctx.setStroke(SEGMENT_PAINT);
                    pocketSegments.forEach(segment -> drawToolpathSegment(ctx, segment));
                }

                layerSegments.addAll(pocketSegments);
                connectedLayerSegments.add(pocketSegments);
            }

            int pocketStartIndex = allSegments.size();
            allSegments.addAll(layerSegments);
            for (int i = pocketStartIndex; i < allSegments.size(); ++i) {
                intersectToolpathSegments(allSegments.get(i), allSegments.subList(0, pocketStartIndex));
                intersectToolpathSegments(allSegments.get(i), allSegments.subList(i + 1, allSegments.size()));
            }

            if (ctx != null && displayMode == DisplayMode.POCKET_SPLIT_POINTS) {
                allSegments.forEach(segment -> drawSplitPoints(ctx, segment));
            }

            List<Toolpath.Segment> validPocketSegments = getAllValidSegments(layerSegments);

            if (ctx != null && displayMode == DisplayMode.POCKET_VALID_SEGMENTS) {
                ctx.setStroke(VALID_PAINT);
                validPocketSegments.forEach(segment -> drawToolpathSegment(ctx, segment));
            }

            List<Toolpath.Segment> insidePocketSegments = validPocketSegments.stream()
                    .filter(segment -> isInsideSegment(connectedPath, segment))
                    .collect(Collectors.toList());

            if (ctx != null && displayMode == DisplayMode.POCKET_INSIDE_OUTSIDE) {
                List<Toolpath.Segment> outsidePocketSegments = validPocketSegments.stream()
                        .filter(segment -> isOutsideSegment(connectedPath, segment))
                        .collect(Collectors.toList());
                drawInsideOutsideSegments(ctx, connectedPath, validPocketSegments, insidePocketSegments,
                        outsidePocketSegments);
            }

            List<Toolpath> partitionedPocketToolpaths = partitionToolpaths(insidePocketSegments);

            pocketToolpaths.addAll(partitionedPocketToolpaths);
            enclosingLayer = partitionedPocketToolpaths;
        }
        Collections.reverse(pocketToolpaths);
        return pocketToolpaths;
    }

    private List<Toolpath> connectPockets(List<Toolpath> pocketToolpaths) {
        List<Toolpath> allConnectedPockets = new ArrayList<>();
        List<PathSegment> allPocketPathSegments = pocketToolpaths.stream()
                .map(ToolpathGenerator::computeEnclosingPath)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        LinkedList<Toolpath> remainingPockets = new LinkedList<>(pocketToolpaths);
        Toolpath currentPocket = remainingPockets.pollFirst();
        while (currentPocket != null) {
            allConnectedPockets.add(currentPocket);
            Point2D currentPoint = currentPocket.getLastSegment().getTo();

            Toolpath nextPocket = null;
            double nextPocketDistance = Double.MAX_VALUE;

            ListIterator<Toolpath> toolpathIterator = remainingPockets.listIterator();
            while (toolpathIterator.hasNext()) {
                Toolpath otherPocket = toolpathIterator.next();
                Point2D startPoint = otherPocket.getLastSegment().getTo();
                LineSegment connection = LineSegment.of(currentPoint, startPoint);
                if (allPocketPathSegments.stream().allMatch(segment -> segment.intersect(connection) == null)) {
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
                currentPocket.setNext(nextPocket);
                currentPocket = nextPocket;
            } else {
                currentPocket = remainingPockets.pollFirst();
            }
        }
        return allConnectedPockets;
    }

    public List<Toolpath> computePocketToolpaths(Direction direction, GraphicsContext ctx, DisplayMode displayMode) {
        List<Toolpath> insideToolpaths = computeProfileToolpaths(Side.INSIDE, direction, ctx, displayMode);
        List<Toolpath> pocketToolpaths = computePockets(insideToolpaths, ctx, displayMode);

        if (ctx != null && displayMode == DisplayMode.POCKET_TOOLPATHS) {
            drawToolpaths(ctx, pocketToolpaths);
        }

        List<Toolpath> connectedPocketToolpaths = connectPockets(pocketToolpaths);

        if (ctx != null && displayMode == DisplayMode.CONNECTED_TOOLPATHS) {
            drawToolpaths(ctx, connectedPocketToolpaths);
        }

        return connectedPocketToolpaths;
    }

    public List<Toolpath> computePocketToolpaths(Direction direction) {
        return computePocketToolpaths(direction, null, null);
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

    private static void drawSegment(GraphicsContext ctx, PathSegment segment) {
        segment.draw(ctx);
    }

    private void drawArc(GraphicsContext ctx, Point2D center, Point2D start, Point2D end) {
        LineSegment centerToStart = LineSegment.of(center, start);
        drawSegment(ctx, centerToStart);
        LineSegment centerToEnd = LineSegment.of(center, end);
        drawSegment(ctx, centerToEnd);
        double radius = centerToStart.getLength();
        double angleToStart = centerToStart.getFromAngle();
        double angleToEnd = centerToEnd.getFromAngle();
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
        if (segment.getSplitPoints().isEmpty()) {
            setValidStroke(ctx, true);
            drawToolpathSegment(ctx, segment);
        } else {
            segment.sortSplitPoints();
            PathSegment remaining = segment.getSegment();
            boolean toSideValid = true;
            for (Toolpath.SplitPoint splitPoint : segment.getSplitPoints()) {
                PathSegment.SplitSegments splitSegments = remaining.split(splitPoint.getPoint());
                setValidStroke(ctx, toSideValid && splitPoint.isFromSideValid());
                drawSegment(ctx, splitSegments.getFromSegment());
                remaining = splitSegments.getToSegment();
                toSideValid = splitPoint.isToSideValid();
            }
            setValidStroke(ctx, toSideValid);
            drawSegment(ctx, remaining);
            ctx.setStroke(PATH_PAINT);
            for (Toolpath.SplitPoint splitPoint : segment.getSplitPoints()) {
                drawPoint(ctx, splitPoint.getPoint());
            }
        }
    }

    private void drawToolpathSegment(GraphicsContext ctx, Toolpath.Segment segment) {
        segment.getSegment().draw(ctx);
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

    private void drawText(GraphicsContext ctx, Point2D textPoint, String text, double angle) {
        ctx.save();
        try {
            textPoint = ctx.getTransform().transform(textPoint);
            Affine transform = new Affine();
            transform.appendTranslation(textPoint.getX(), textPoint.getY());
            transform.appendRotation(360 - Math2D.convertToDegrees(angle));
            ctx.setTransform(transform);
            ctx.setLineWidth(1);
            ctx.setTextAlign(TextAlignment.CENTER);
            ctx.strokeText(text, 0, -5);
        } finally {
            ctx.restore();
        }
    }

    private void drawPointInsidePath(GraphicsContext ctx, List<PathSegment> path, Point2D point) {
        if (PathSegment.isPointInsidePath(path, point)) {
            ctx.setFill(INSIDE_PAINT);
        } else {
            ctx.setFill(OUTSIDE_PAINT);
        }
        drawPoint(ctx, point);
        ctx.setFill(PATH_PAINT);
    }

    private void drawInsideOutsideSegments(GraphicsContext ctx, List<PathSegment> connectedEdges,
                                           List<Toolpath.Segment> allValidSegments,
                                           List<Toolpath.Segment> insideSegments,
                                           List<Toolpath.Segment> outsideSegments) {
        ctx.setStroke(INSIDE_PAINT);
        insideSegments.forEach(segment -> drawToolpathSegment(ctx, segment));
        ctx.setStroke(OUTSIDE_PAINT);
        outsideSegments.forEach(segment -> drawToolpathSegment(ctx, segment));
        allValidSegments.stream().forEach(segment -> {
            drawPointInsidePath(ctx, connectedEdges, segment.getFrom());
            drawPointInsidePath(ctx, connectedEdges, segment.getTo());
        });
    }

    private void drawToolpaths(GraphicsContext ctx, List<Toolpath> toolpaths) {
        ctx.setStroke(VALID_PAINT);
        int toolpathIndex = 0;
        for (Toolpath toolpath : toolpaths) {
            ++toolpathIndex;

            boolean closed = isToolpathClosed(toolpath.getSegments());

            Direction toolpathDirection = toolpath.getDirection();
            String directionText = (toolpathDirection == Direction.CLOCKWISE) ? "CW" : "CCW";

            Toolpath.Segment prevSegment = toolpath.getLastSegment();
            int segmentIndex = 0;
            for (Toolpath.Segment segment : toolpath.getSegments()) {
                ++segmentIndex;

                Point2D textPoint = segment.getFrom().midpoint(segment.getTo());
                String text;
                if (directionText != null) {
                    text = String.format("%d.%d (%s)", toolpathIndex, segmentIndex, directionText);
                    directionText = null;
                } else {
                    text = String.format("%d.%d", toolpathIndex, segmentIndex);
                }
                drawText(ctx, textPoint, text, segment.getSegment().getFromAngle());

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

            if (toolpath.hasNext()) {
                drawLine(ctx, toolpath.getLastSegment().getTo(),
                        toolpath.getNext().getLastSegment().getTo());
            }
        }
    }

}
