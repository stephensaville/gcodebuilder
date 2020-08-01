package com.gcodebuilder.app;

import com.gcodebuilder.geometry.Segment;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PathBuilderController {
    private static final double POINT_RADIUS = 5;
    private static final double TOOL_WIDTH = 100;
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

    // rotate vector by angle expressed in radians
    private static Point2D rotate(Point2D vec, double angle) {
        double sinAngle = Math.sin(angle);
        double cosAngle = Math.cos(angle);
        return new Point2D(cosAngle*vec.getX() - sinAngle*vec.getY(),
                sinAngle*vec.getX() + cosAngle*vec.getY());
    }

    private static double unitVecToAngle(Point2D unitVec) {
        double angle = Math.acos(unitVec.getX());
        if (unitVec.getY() < 0) {
            angle = 2*Math.PI - angle;
        }
        return angle;
    }

    private static Point2D rotateLeft90(Point2D vec) {
        return new Point2D(-vec.getY(), vec.getX());
    }

    private static Point2D rotateRight90(Point2D vec) {
        return new Point2D(vec.getY(), -vec.getX());
    }

    private static Segment moveLineSegment(Point2D fromPoint, Point2D toPoint, Point2D offset) {
        return Segment.of(fromPoint.add(offset), toPoint.add(offset));
    }

    private List<Segment> computeToolpathSegments(Point2D fromPoint, Point2D toPoint, double toolRadius) {
        Point2D lineVec = toPoint.subtract(fromPoint).normalize();
        Point2D leftOffset = rotateLeft90(lineVec).multiply(toolRadius);
        Point2D rightOffset = rotateRight90(lineVec).multiply(toolRadius);
        return Arrays.asList(moveLineSegment(fromPoint, toPoint, leftOffset),
                moveLineSegment(fromPoint, toPoint, rightOffset));
    }

    private void intersectToolpathSegments(int i, List<Segment> segments) {
        Segment current = segments.get(i);
        for (int j = i + 1; j < segments.size(); ++j) {
            Segment other = segments.get(j);
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
            List<Segment> leftToolpathSegments = new ArrayList<>();
            List<Segment> rightToolpathSegments = new ArrayList<>();
            for (int i = 0; i < nPoints; ++i) {
                Point2D prevPoint = points.get((i+nPoints-1) % nPoints);
                Point2D thisPoint = points.get(i);
                Point2D nextPoint = points.get((i+1) % nPoints);

                drawPoint(thisPoint);
                drawLine(prevPoint, thisPoint);

                List<Segment> toolpathSegments = computeToolpathSegments(thisPoint, nextPoint, TOOL_WIDTH/2);
                for (Segment segment : toolpathSegments) {
                    drawLine(segment);
                }
                leftToolpathSegments.add(toolpathSegments.get(0));
                rightToolpathSegments.add(toolpathSegments.get(1));
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
