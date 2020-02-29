package com.gcodebuilder.app;

import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;

import java.util.ArrayList;
import java.util.List;

public class PathBuilderController {
    private static final double POINT_RADIUS = 5;
    private static final double TOOL_WIDTH = 20;
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

    private void drawTrace(Point2D fromPoint, Point2D toPoint, Point2D offsetVec) {
        double lineLength = fromPoint.distance(toPoint);
        double step = Math.max(TRACE_STEP / lineLength, 0.0001);
        for (double v = 0; v <= 1; v += step) {
            Point2D point = fromPoint.interpolate(toPoint, v).add(offsetVec);
            if (isPointInPath(point)) {
                drawPoint(point);
            }
        }
    }

    private void tracePath(Point2D fromPoint, Point2D toPoint) {
        Point2D midPoint = toPoint.midpoint(fromPoint);
        Point2D vec = toPoint.subtract(fromPoint).normalize();
        Point2D lVec = new Point2D(-vec.getY(), vec.getX()).multiply(2*POINT_RADIUS);
        Point2D rVec = new Point2D(vec.getY(), -vec.getX()).multiply(2*POINT_RADIUS);
        Point2D lPoint = midPoint.add(lVec);
        if (isPointInPath(lPoint)) {
            drawPoint(lPoint);
            drawTrace(fromPoint, toPoint, lVec);
        }
        Point2D rPoint = midPoint.add(rVec);
        if (isPointInPath(rPoint)) {
            drawPoint(rPoint);
            drawTrace(fromPoint, toPoint, rVec);
        }
    }

    // rotate vector by angle expressed in radians
    private static Point2D rotate(Point2D vec, double angle) {
        double sinAngle = Math.sin(angle);
        double cosAngle = Math.cos(angle);
        return new Point2D(cosAngle*vec.getX() - sinAngle*vec.getY(),
                sinAngle*vec.getX() + cosAngle*vec.getY());
    }

    private static Point2D rotate90(Point2D vec) {
        return new Point2D(-vec.getY(), vec.getX());
    }

    private static double unitVecToAngle(Point2D unitVec) {
        double angle = Math.acos(unitVec.getX());
        if (unitVec.getY() < 0) {
            angle = 2*Math.PI - angle;
        }
        return angle;
    }

    private Point2D findInsideCorner(Point2D thisPoint, Point2D lineVec, double halfAngle, double halfWidth) {
        double pOffset = halfWidth / Math.tan(halfAngle);
        Point2D orthoVec = rotate90(lineVec);
        return thisPoint.add(lineVec.multiply(pOffset)).add(orthoVec.multiply(halfWidth));
    }

    private Point2D findOutsideCorner(Point2D thisPoint, Point2D lineVec, double halfAngle, double halfWidth) {
        Point2D cornerVec = rotate(lineVec, halfAngle).multiply(halfWidth);
        return thisPoint.add(cornerVec);
    }

    private Point2D findCorner(Point2D prevPoint, Point2D thisPoint, Point2D nextPoint, double toolWidth) {
        Point2D insideCorner, outsideCorner;
        double halfWidth = toolWidth / 2;

        Point2D vecToPrev = prevPoint.subtract(thisPoint).normalize();
        double angleToPrev = unitVecToAngle(vecToPrev);

        Point2D vecToNext = nextPoint.subtract(thisPoint).normalize();
        double angleToNext = unitVecToAngle(vecToNext);

        double smallAngle, largeAngle;
        Point2D smallVec, largeVec;
        if (angleToPrev < angleToNext) {
            smallAngle = angleToPrev;
            smallVec = vecToPrev;
            largeAngle = angleToNext;
            largeVec = vecToNext;
        } else {
            smallAngle = angleToNext;
            smallVec = vecToNext;
            largeAngle = angleToPrev;
            largeVec = vecToPrev;
        }
        double firstAngle = largeAngle - smallAngle;
        double secondAngle = 2*Math.PI - firstAngle;

        if (firstAngle < Math.PI) {
            // inside corner first
            insideCorner = findInsideCorner(thisPoint, smallVec, firstAngle / 2, halfWidth);
            outsideCorner = findOutsideCorner(thisPoint, largeVec, secondAngle / 2, halfWidth);
        } else if (firstAngle > Math.PI) {
            // outside corner first
            outsideCorner = findOutsideCorner(thisPoint, smallVec, firstAngle / 2, halfWidth);
            insideCorner = findInsideCorner(thisPoint, largeVec, secondAngle / 2, halfWidth);
        } else {
            // straight line
            insideCorner = thisPoint.add(rotate90(smallVec).multiply(halfWidth));
            outsideCorner = thisPoint.add(rotate90(largeVec).multiply(halfWidth));
        }

        if (isPointInPath(insideCorner)) {
            return insideCorner;
        } else {
            return outsideCorner;
        }
    }

    private void redrawPath() {
        System.out.println("redrawPath");

        ctx.clearRect(0, 0, pathCanvas.getWidth(), pathCanvas.getHeight());

        if (pathClosed) {
            int nPoints = points.size();
            for (int i = 0; i < nPoints; ++i) {
                Point2D prevPoint = points.get((i+nPoints-1) % nPoints);
                Point2D thisPoint = points.get(i);
                Point2D nextPoint = points.get((i+1) % nPoints);

                drawPoint(thisPoint);
                drawLine(prevPoint, thisPoint);
                Point2D cornerPoint = findCorner(prevPoint, thisPoint, nextPoint, TOOL_WIDTH);
                drawCircle(cornerPoint, TOOL_WIDTH/2);
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
