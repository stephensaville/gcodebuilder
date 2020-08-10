package com.gcodebuilder.app;

import com.gcodebuilder.generator.ToolpathGenerator;
import com.gcodebuilder.generator.ToolpathGenerator.DisplayMode;
import com.gcodebuilder.geometry.Path;
import com.gcodebuilder.geometry.Point;
import javafx.beans.binding.DoubleBinding;
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

    private void redrawPath() {
        ctx.clearRect(0, 0, pathCanvas.getWidth(), pathCanvas.getHeight());

        Path path = new Path();
        points.forEach(point -> path.addPoint(new Point(point)));
        path.setClosed(pathClosed);

        ToolpathGenerator generator = new ToolpathGenerator();
        generator.addPath(path);
        generator.drawToolpath(ctx, displayMode);
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
            return;
        }

        if (currentPointIndex == points.size()) {
            points.add(startPoint);
        } else if (currentPointIndex == 0) {
            pathClosed = true;
        }

        redrawPath();
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
