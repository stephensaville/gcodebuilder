package com.gcodebuilder.app;

import com.gcodebuilder.app.tools.Tool;
import com.gcodebuilder.geometry.Segment;
import com.gcodebuilder.geometry.UnitVector;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PathBuilderController {
    private static final double POINT_RADIUS = 5;
    private static final double TOOL_WIDTH = 100;
    private static final double TOOL_RADIUS = TOOL_WIDTH / 2;
    private static final double TRACE_STEP = 5;

    @FXML private BorderPane rootPane;
    @FXML private Canvas pathCanvas;

    private GraphicsContext ctx;
    private List<Point2D> points = new ArrayList<>();
    private Point2D startPoint = null;
    private int currentPointIndex = 0;
    private boolean pathClosed = false;

    @FXML
    public void initialize() {
        System.out.println("initializing...");

        pathCanvas.widthProperty().bind(rootPane.widthProperty());
        pathCanvas.heightProperty().bind(rootPane.heightProperty());

        ctx = pathCanvas.getGraphicsContext2D();
    }

    private void drawCircle(Point2D point, double radius) {
        ctx.fillOval(point.getX() - radius, point.getY() - radius, radius*2, radius*2);
    }

    private void drawPoint(Point2D point2D) {
        drawCircle(point2D, POINT_RADIUS);
    }

    private void drawLine(Point2D fromPoint, Point2D toPoint) {
        ctx.strokeLine(fromPoint.getX(), fromPoint.getY(),
                toPoint.getX(), toPoint.getY());
    }

    private void drawLine(Segment segment) {
        drawLine(segment.getFrom(), segment.getTo());
    }

    private Point2D firstPoint() {
        return points.get(0);
    }

    private Point2D lastPoint() {
        return points.get(points.size() - 1);
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
    private static class ToolpathSegment {
        private final Segment segment;
        private final UnitVector towards;
        private final UnitVector away;
        private final boolean valid;

        public Point2D intersect(ToolpathSegment other) {
            return segment.intersect(other.segment);
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
                new ToolpathSegment(edge.move(left.multiply(toolRadius)), right, left, true),
                new ToolpathSegment(edge.move(right.multiply(toolRadius)), left, right, true)
        };
    }

    private void intersectToolpathSegments(int i, List<ToolpathSegment> toolpathSegments) {
        ToolpathSegment current = toolpathSegments.get(i);
        for (int j = i + 1; j < toolpathSegments.size(); ++j) {
            ToolpathSegment other = toolpathSegments.get(j);
            Point2D intersectionPoint = current.intersect(other);
            if (intersectionPoint != null) {
                drawPoint(intersectionPoint);
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
                Point2D nextPoint = points.get((i+1) % nPoints);

                drawPoint(thisPoint);

                Segment edge = Segment.of(prevPoint, thisPoint);
                drawLine(edge);

                ToolpathSegment[] toolpathSegments = computeToolpathSegments(edge, TOOL_RADIUS);
                for (ToolpathSegment segment : toolpathSegments) {
                    drawLine(segment.getSegment());
                }
                leftToolpathSegments.add(toolpathSegments[0]);
                rightToolpathSegments.add(toolpathSegments[1]);
            }
            for (int i = 0; i < nPoints; ++i) {
                intersectToolpathSegments(i, leftToolpathSegments);
                intersectToolpathSegments(i, rightToolpathSegments);
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
}
