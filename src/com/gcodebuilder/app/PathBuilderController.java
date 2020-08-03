package com.gcodebuilder.app;

import com.gcodebuilder.geometry.Math2D;
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
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PathBuilderController {
    private static final double POINT_RADIUS = 5;
    private static final double TOOL_WIDTH = 100;
    private static final double TOOL_RADIUS = TOOL_WIDTH / 2;
    private static final double TRACE_STEP = 5;

    private static final Paint DEFAULT_PAINT = Color.BLACK;
    private static final Paint VALID_PAINT = Color.GREEN;
    private static final Paint INVALID_PAINT = Color.RED;

    private enum DisplayMode {
        SPLIT_POINTS,
        VALID_SEGMENTS
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

        public static ToolpathSegment fromEdge(Segment edge, double toolRadius, UnitVector towards, UnitVector away) {
            return new ToolpathSegment(edge.move(away.multiply(toolRadius)), toolRadius, towards, away);
        }

        private final List<ToolpathSplitPoint> splitPoints = new ArrayList<>();
        private boolean splitPointsSorted = true;

        public Point2D intersect(ToolpathSegment other) {
            return segment.intersect(other.segment);
        }

        public void split(Point2D splitPoint, boolean fromSideValid, boolean toSideValid) {
            splitPoints.add(new ToolpathSplitPoint(splitPoint, fromSideValid, toSideValid));
            splitPointsSorted = false;
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

        ctx.save();
        ctx.setLineWidth(2);
        drawLine(edge.getFrom(), edge.getFrom().add(edge.getDirection().multiply(toolRadius)));
        drawLine(edge.getFrom(), edge.getFrom().add(left.multiply(toolRadius)));
        drawLine(edge.getFrom(), edge.getFrom().add(right.multiply(toolRadius)));
        ctx.restore();

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
            for (int i = 0; i < leftToolpathSegments.size(); ++i) {
                intersectToolpathSegments(i, leftToolpathSegments);
            }
            for (int i = 0; i < rightToolpathSegments.size(); ++i) {
                intersectToolpathSegments(i, rightToolpathSegments);
            }
            if (displayMode == DisplayMode.SPLIT_POINTS) {
                leftToolpathSegments.forEach(this::drawSplitPoints);
                rightToolpathSegments.forEach(this::drawSplitPoints);
            } else {
                List<ToolpathSegment> leftValidSegments = getAllValidSegments(leftToolpathSegments);
                List<ToolpathSegment> rightValidSegments = getAllValidSegments(rightToolpathSegments);
                if (displayMode == DisplayMode.VALID_SEGMENTS) {
                    ctx.save();
                    ctx.setStroke(VALID_PAINT);
                    leftValidSegments.forEach(this::drawToolpathSegment);
                    rightValidSegments.forEach(this::drawToolpathSegment);
                    ctx.restore();
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

    public void resetPath() {
        points.clear();
        startPoint = null;
        currentPointIndex = 0;
        pathClosed = false;
        redrawPath();
    }
}
