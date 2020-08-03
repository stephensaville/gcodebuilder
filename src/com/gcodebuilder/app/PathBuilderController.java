package com.gcodebuilder.app;

import com.gcodebuilder.app.tools.Tool;
import com.gcodebuilder.geometry.Math2D;
import com.gcodebuilder.geometry.Point;
import com.gcodebuilder.geometry.Segment;
import com.gcodebuilder.geometry.UnitVector;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class PathBuilderController {
    private static final Logger log = LogManager.getLogger(PathBuilderController.class);

    private static final double POINT_RADIUS = 5;
    private static final double TOOL_WIDTH = 100;
    private static final double TOOL_RADIUS = TOOL_WIDTH / 2;
    private static final double MIN_POINT_DISTANCE = 0.0001;

    private static final Paint DEFAULT_PAINT = Color.BLACK;
    private static final Paint VALID_PAINT = Color.GREEN;
    private static final Paint INVALID_PAINT = Color.RED;
    private static final Paint INSIDE_PAINT = Color.PURPLE;
    private static final Paint OUTSIDE_PAINT = Color.DARKORANGE;

    private enum DisplayMode {
        SPLIT_POINTS,
        VALID_SEGMENTS,
        PARTITIONED_TOOLPATHS,
        INSIDE_OUTSIDE
    }

    @FXML private BorderPane rootPane;
    @FXML private Canvas pathCanvas;

    private GraphicsContext ctx;
    private List<Point2D> points = new ArrayList<>();
    private Point2D startPoint = null;
    private int currentPointIndex = 0;
    private boolean pathClosed = false;
    private DisplayMode displayMode = DisplayMode.SPLIT_POINTS;

    @FXML
    public void initialize() {
        ctx = pathCanvas.getGraphicsContext2D();
    }

    public void bindProperties() {
        pathCanvas.widthProperty().bind(rootPane.widthProperty());
        pathCanvas.heightProperty().bind(rootPane.heightProperty());

        DoubleBinding widthBinding = rootPane.widthProperty()
                .subtract(NodeSize.measureWidth(rootPane.getLeft()))
                .subtract(NodeSize.measureWidth(rootPane.getRight()));
        pathCanvas.widthProperty().bind(widthBinding);

        DoubleBinding heightBinding = rootPane.heightProperty()
                .subtract(NodeSize.measureHeight(rootPane.getTop()))
                .subtract(NodeSize.measureHeight(rootPane.getBottom()));
        pathCanvas.heightProperty().bind(heightBinding);
    }

    /**
     * Checks if the given point is inside the currently closed path using the even-off winding rule.
     *
     * @param point point to check
     * @return true if point is inside, false if point is outside
     */
    private boolean isPointInPath(Point2D point) {
        if (!pathClosed) {
            return false;
        }
        boolean pointInPath = false;
        Point2D adjPoint = lastPoint();
        for (Point2D thisPoint : points) {
            /* Checking:
             *   1. If a horizontal line through point intersects with the line segment between thisPoint and adjPoint.
             *   2. If the x coordinate of the point is less than the x coordinate of the point where a horizontal line
             *      through point intersects the line segment between thisPoint and adjPoint.
             */
            if ((adjPoint.getY() > point.getY()) != (thisPoint.getY() > point.getY()) &&
                    (point.getX() < thisPoint.getX() + (point.getY() - thisPoint.getY())
                            * (adjPoint.getX() - thisPoint.getX())
                            / (adjPoint.getY() - thisPoint.getY()))) {
                pointInPath = !pointInPath;
            }
            adjPoint = thisPoint;
        }
        return pointInPath;
    }

    @Data
    private static class ToolpathSplitPoint {
        private final Point2D point;
        private final boolean fromSideValid;
        private final boolean toSideValid;
    }

    @RequiredArgsConstructor
    private static class ToolpathSplitPointComparator implements Comparator<ToolpathSplitPoint> {
        private final Point2D from;

        @Override
        public int compare(ToolpathSplitPoint left, ToolpathSplitPoint right) {
            double leftDistance = from.distance(left.getPoint());
            double rightDistance = from.distance(right.getPoint());
            if (leftDistance < rightDistance) {
                return -1;
            } else if (leftDistance > rightDistance) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class ToolpathSegment {
        private final Segment segment;
        private final double toolRadius;
        private final UnitVector towards;
        private final UnitVector away;

        private final List<ToolpathSplitPoint> splitPoints = new ArrayList<>();
        private boolean splitPointsSorted = true;

        public static ToolpathSegment fromEdge(Segment edge, double toolRadius, UnitVector towards, UnitVector away) {
            return new ToolpathSegment(edge.move(away.multiply(toolRadius)), toolRadius, towards, away);
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

        public Point2D getEdgePoint(Point2D segmentPoint) {
            return segmentPoint.add(towards.multiply(toolRadius));
        }

        public void split(Point2D splitPoint, boolean fromSideValid, boolean toSideValid) {
            splitPoints.add(new ToolpathSplitPoint(splitPoint, fromSideValid, toSideValid));
            splitPointsSorted = false;
        }

        public ToolpathSegment flip() {
            return new ToolpathSegment(segment.flip(), toolRadius, towards, away);
        }

        public void sortSplitPoints() {
            if (!splitPointsSorted) {
                splitPoints.sort(new ToolpathSplitPointComparator(segment.getFrom()));
                splitPointsSorted = true;
            }
        }

        private static void setStroke(GraphicsContext ctx, boolean valid) {
            if (valid) {
                ctx.setStroke(VALID_PAINT);
            } else {
                ctx.setStroke(INVALID_PAINT);
            }
        }

        public void drawSplitPoints(GraphicsContext ctx) {
            sortSplitPoints();
            ctx.save();
            Point2D lastSplitPoint = segment.getFrom();
            boolean toSideValid = true;
            for (ToolpathSplitPoint splitPoint : splitPoints) {
                setStroke(ctx, splitPoint.isFromSideValid());
                drawLine(ctx, lastSplitPoint, splitPoint.getPoint());
                lastSplitPoint = splitPoint.getPoint();
                toSideValid = splitPoint.isToSideValid();
            }
            setStroke(ctx, toSideValid);
            drawLine(ctx, lastSplitPoint, segment.getTo());
            ctx.setStroke(DEFAULT_PAINT);
            for (ToolpathSplitPoint splitPoint : splitPoints) {
                drawPoint(ctx, splitPoint.getPoint());
            }
            ctx.restore();
        }

        public List<ToolpathSegment> getValidSegments() {
            List<ToolpathSegment> validSegments = new ArrayList<>();
            sortSplitPoints();
            Point2D lastSplitPoint = segment.getFrom();
            boolean toSideValid = true;
            for (ToolpathSplitPoint splitPoint : splitPoints) {
                if (splitPoint.isFromSideValid()) {
                    validSegments.add(new ToolpathSegment(
                            Segment.of(lastSplitPoint, splitPoint.getPoint()),
                            toolRadius, towards, away));
                }
                lastSplitPoint = splitPoint.getPoint();
                toSideValid = splitPoint.isToSideValid();
            }
            if (toSideValid) {
                validSegments.add(new ToolpathSegment(
                        Segment.of(lastSplitPoint, segment.getTo()),
                        toolRadius, towards, away));
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

    private void intersectToolpathSegments(int i, List<ToolpathSegment> toolpathSegments) {
        ToolpathSegment current = toolpathSegments.get(i);
        for (int j = i + 1; j < toolpathSegments.size(); ++j) {
            ToolpathSegment other = toolpathSegments.get(j);
            Point2D intersectionPoint = current.intersect(other);
            if (intersectionPoint != null) {
                double angleFromCurrentToTowards = Math.abs(Math2D.subtractAngle(
                        other.getTowards().getAngle(), current.getSegment().getDirection().getAngle()));
                boolean currentFromSideValid = (angleFromCurrentToTowards <= Math.PI/2);
                current.split(intersectionPoint, currentFromSideValid, !currentFromSideValid);

                double angleFromOtherToTowards = Math.abs(Math2D.subtractAngle(
                        current.getTowards().getAngle(), other.getSegment().getDirection().getAngle()));
                boolean otherFromSideValid = (angleFromOtherToTowards <= Math.PI/2);
                other.split(intersectionPoint, otherFromSideValid, !otherFromSideValid);
            }
        }
    }

    private static List<ToolpathSegment> getAllValidSegments(List<ToolpathSegment> toolpathSegments) {
        return toolpathSegments.stream()
                .flatMap(toolpathSegment -> toolpathSegment.getValidSegments().stream())
                .collect(Collectors.toList());
    }

    private static boolean isSamePoint(Point2D p1, Point2D p2) {
        if (p1.equals(p2)) {
            return true;
        } else {
            return p1.distance(p2) < MIN_POINT_DISTANCE;
        }
    }

    private static boolean isToolpathClosed(List<ToolpathSegment> toolpath) {
        if (toolpath.size() <= 1) {
            return false;
        }
        ToolpathSegment first = toolpath.get(0);
        ToolpathSegment last = toolpath.get(toolpath.size() - 1);
        if (isSamePoint(first.getFrom(), last.getTo())) {
            return true;
        }
        Point2D firstEdgePoint = first.getEdgePoint(first.getFrom());
        Point2D lastEdgePoint = last.getEdgePoint(last.getTo());
        if (isSamePoint(firstEdgePoint, lastEdgePoint)) {
            return true;
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

            // first look for an intersection point (inside corner)
            Iterator<ToolpathSegment> segmentIterator = remainingSegments.iterator();
            while (segmentIterator.hasNext()) {
                ToolpathSegment other = segmentIterator.next();
                if (isSamePoint(current.getTo(), other.getFrom())) {
                    next = other;
                    segmentIterator.remove();
                    break;
                }
                if (isSamePoint(current.getTo(), other.getTo())) {
                    next = other.flip();
                    segmentIterator.remove();
                    break;
                }
            }

            // next look for same edge point (outside corner)
            if (next == null) {
                Point2D currentEdgePoint = current.getEdgePoint(current.getTo());
                segmentIterator = remainingSegments.iterator();
                while (segmentIterator.hasNext()) {
                    ToolpathSegment other = segmentIterator.next();
                    Point2D otherFromEdgePoint = other.getEdgePoint(other.getFrom());
                    if (isSamePoint(currentEdgePoint, otherFromEdgePoint)) {
                        next = other;
                        segmentIterator.remove();
                        break;
                    }
                    Point2D otherToEdgePoint = other.getEdgePoint(other.getTo());
                    if (isSamePoint(currentEdgePoint, otherToEdgePoint)) {
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
                if (isToolpathClosed(currentToolpath)) {
                    result.add(currentToolpath);
                }
                currentToolpath = new ArrayList<>();
                current = remainingSegments.pollFirst();
            }
        }
        return result;
    }

    private boolean isInsideSegment(ToolpathSegment segment) {
        if (!isPointInPath(segment.getFrom())) {
            return false;
        }
        if (!isPointInPath(segment.getTo())) {
            return false;
        }
        return true;
    }

    private boolean isOutsideSegment(ToolpathSegment segment) {
        if (isPointInPath(segment.getFrom())) {
            return false;
        }
        if (isPointInPath(segment.getTo())) {
            return false;
        }
        return true;
    }

    private static void drawCircle(GraphicsContext ctx, Point2D center, double radius) {
        ctx.fillOval(center.getX() - radius, center.getY() - radius, radius*2, radius*2);
    }

    private void drawCircle(Point2D center, double radius) {
        drawCircle(ctx, center, radius);
    }

    private static void drawPoint(GraphicsContext ctx, Point2D point) {
        drawCircle(ctx, point, POINT_RADIUS);
    }

    private void drawPoint(Point2D point) {
        drawPoint(ctx, point);
    }

    private static void drawLine(GraphicsContext ctx, Point2D fromPoint, Point2D toPoint) {
        ctx.strokeLine(fromPoint.getX(), fromPoint.getY(),
                toPoint.getX(), toPoint.getY());
    }

    private void drawLine(Point2D fromPoint, Point2D toPoint) {
        drawLine(ctx, fromPoint, toPoint);
    }

    private static void drawLine(GraphicsContext ctx, Segment segment) {
        drawLine(ctx, segment.getFrom(), segment.getTo());
    }

    private void drawLine(Segment segment) {
        drawLine(ctx, segment);
    }

    private void drawArc(Point2D center, Point2D start, Point2D end) {
        Segment centerToStart = Segment.of(center, start);
        drawLine(centerToStart);
        Segment centerToEnd = Segment.of(center, end);
        drawLine(centerToEnd);
        double radius = centerToStart.getLength();
        double angleToStart = centerToStart.getDirection().getAngle();
        double angleToEnd = centerToEnd.getDirection().getAngle();
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

    private Point2D firstPoint() {
        return points.get(0);
    }

    private Point2D lastPoint() {
        return points.get(points.size() - 1);
    }

    private void drawSplitPoints(ToolpathSegment toolpathSegment) {
        toolpathSegment.drawSplitPoints(ctx);
    }

    private void drawToolpathSegment(ToolpathSegment toolpathSegment) {
        drawLine(toolpathSegment.getSegment());
    }

    private void drawPartitionedToolpaths(List<List<ToolpathSegment>> toolpaths) {
        int toolpathIndex = 0;
        for (List<ToolpathSegment> toolpath : toolpaths) {
            ++toolpathIndex;

            ToolpathSegment prevSegment = toolpath.get(toolpath.size() - 1);
            for (ToolpathSegment segment : toolpath) {
                Point2D textOffset = segment.getTowards().multiply(segment.getToolRadius() / 3);
                Point2D textPoint = segment.getFrom()
                        .midpoint(segment.getTo())
                        .add(textOffset);
                ctx.strokeText(String.valueOf(toolpathIndex), textPoint.getX(), textPoint.getY());

                if (!isSamePoint(prevSegment.getTo(), segment.getFrom())) {
                    // draw an arc to join outside corner segments
                    drawArc(segment.getEdgePoint(segment.getFrom()), prevSegment.getTo(), segment.getFrom());
                }
                drawToolpathSegment(segment);

                prevSegment = segment;
            }
        }
    }

    private void redrawPath() {
        System.out.println("redrawPath");

        ctx.clearRect(0, 0, pathCanvas.getWidth(), pathCanvas.getHeight());

        if (pathClosed) {
            int nPoints = points.size();
            List<ToolpathSegment> leftToolpathSegments = new ArrayList<>();
            List<ToolpathSegment> rightToolpathSegments = new ArrayList<>();
            for (int i = 0; i < nPoints; ++i) {
                Point2D prevPoint = points.get((i+nPoints-1) % nPoints);
                Point2D thisPoint = points.get(i);

                drawPoint(thisPoint);

                Segment edge = Segment.of(prevPoint, thisPoint);
                drawLine(edge);

                ToolpathSegment[] toolpathSegments = computeToolpathSegments(edge, TOOL_RADIUS);
                leftToolpathSegments.add(toolpathSegments[0]);
                rightToolpathSegments.add(toolpathSegments[1]);
            }
            List<ToolpathSegment> allToolpathSegments = new ArrayList<>();
            allToolpathSegments.addAll(leftToolpathSegments);
            allToolpathSegments.addAll(rightToolpathSegments);
            for (int i = 0; i < allToolpathSegments.size(); ++i) {
                intersectToolpathSegments(i, allToolpathSegments);
            }
            if (displayMode == DisplayMode.SPLIT_POINTS) {
                allToolpathSegments.forEach(this::drawSplitPoints);
            } else {
                List<ToolpathSegment> allValidSegments = getAllValidSegments(allToolpathSegments);
                if (displayMode == DisplayMode.VALID_SEGMENTS) {
                    ctx.save();
                    ctx.setStroke(VALID_PAINT);
                    allValidSegments.forEach(this::drawToolpathSegment);
                    ctx.restore();
                } else {
                    List<ToolpathSegment> insideSegments = allValidSegments.stream()
                            .filter(this::isInsideSegment).collect(Collectors.toList());
                    List<ToolpathSegment> outsideSegments = allValidSegments.stream()
                            .filter(this::isOutsideSegment).collect(Collectors.toList());
                    if (displayMode == DisplayMode.INSIDE_OUTSIDE) {
                        ctx.save();
                        ctx.setStroke(INSIDE_PAINT);
                        insideSegments.forEach(this::drawToolpathSegment);
                        ctx.setStroke(OUTSIDE_PAINT);
                        outsideSegments.forEach(this::drawToolpathSegment);
                        ctx.restore();
                    } else {
                        List<List<ToolpathSegment>> insideToolpaths = partitionToolpaths(insideSegments);
                        List<List<ToolpathSegment>> outsideToolpaths = partitionToolpaths(outsideSegments);
                        if (displayMode == DisplayMode.PARTITIONED_TOOLPATHS) {
                            ctx.save();
                            ctx.setStroke(INSIDE_PAINT);
                            drawPartitionedToolpaths(insideToolpaths);
                            ctx.setStroke(OUTSIDE_PAINT);
                            drawPartitionedToolpaths(outsideToolpaths);
                            ctx.restore();
                        }
                    }
                }
            }
        } else {
            Point2D prevPoint = null;
            for (Point2D point : points) {
                drawPoint(point);
                if (prevPoint != null) {
                    drawLine(prevPoint, point);
                }
                prevPoint = point;
            }
        }
    }

    public void mousePressOnCanvas(MouseEvent ev) {
        startPoint = new Point2D(ev.getX(), ev.getY());
        currentPointIndex = points.size();

        for (int pointIndex = 0; pointIndex < currentPointIndex; ++pointIndex) {
            if (startPoint.distance(points.get(pointIndex)) <= POINT_RADIUS) {
                currentPointIndex = pointIndex;
            }
        }

        if (pathClosed) {
            if (currentPointIndex == points.size()) {
                if (isPointInPath(startPoint)) {
                    System.out.println("inside");
                } else {
                    System.out.println("outside");
                }
            }
            return;
        }

        if (currentPointIndex == points.size()) {
            // add new point
            points.add(startPoint);

            drawPoint(startPoint);
            if (currentPointIndex > 0) {
                Point2D prevPoint = points.get(currentPointIndex - 1);
                drawLine(prevPoint, startPoint);
            }
        } else if (currentPointIndex == 0) {
            // close path
            pathClosed = true;

            drawLine(lastPoint(), firstPoint());
        }
    }

    private Point2D moveCurrentPoint(MouseEvent ev) {
        Point2D dragPoint = new Point2D(ev.getX(), ev.getY());
        Point2D offset = dragPoint.subtract(startPoint);
        return points.get(currentPointIndex).add(offset);
    }

    public void mouseReleaseOnCanvas(MouseEvent ev) {
        if (startPoint != null && currentPointIndex < points.size()) {
            points.set(currentPointIndex, moveCurrentPoint(ev));
            redrawPath();
        }
    }

    public void mouseDragOnCanvas(MouseEvent ev) {
        if (startPoint != null && currentPointIndex < points.size()) {
            Point2D origPoint = points.get(currentPointIndex);
            Point2D movedPoint = moveCurrentPoint(ev);
            points.set(currentPointIndex, movedPoint);
            redrawPath();
            points.set(currentPointIndex, origPoint);
        }
    }

    public void displaySplitPoints() {
        displayMode = DisplayMode.SPLIT_POINTS;
        redrawPath();
    }

    public void displayValidSegments() {
        displayMode = DisplayMode.VALID_SEGMENTS;
        redrawPath();
    }

    public void displayPartitionedToolpaths() {
        displayMode = DisplayMode.PARTITIONED_TOOLPATHS;
        redrawPath();
    }

    public void displayInsideOutside() {
        displayMode = DisplayMode.INSIDE_OUTSIDE;
        redrawPath();
    }

    public void resetPath() {
        points.clear();
        startPoint = null;
        currentPointIndex = 0;
        pathClosed = false;
        redrawPath();
    }
}
