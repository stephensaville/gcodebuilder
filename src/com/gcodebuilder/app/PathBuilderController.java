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
    private ToolpathGenerator generator = new ToolpathGenerator();
    private List<Path> paths = new ArrayList<>();
    private Path currentPath;
    private Point2D startPoint;
    private int currentPointIndex;
    private DisplayMode displayMode = DisplayMode.SPLIT_POINTS;

    public PathBuilderController() {
        currentPath = new Path();
        paths.add(currentPath);
        generator.addPath(currentPath);
    }

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
        generator.drawToolpath(ctx, displayMode);
    }

    public void mousePressOnCanvas(MouseEvent ev) {
        startPoint = new Point2D(ev.getX(), ev.getY());
        currentPointIndex = currentPath.getPointCount();

        for (int pointIndex = 0; pointIndex < currentPointIndex; ++pointIndex) {
            if (currentPath.getPoint(pointIndex).isSame(startPoint, generator.getPointRadius())) {
                currentPointIndex = pointIndex;
            }
        }

        if (currentPath.isClosed()) {
            return;
        }

        if (currentPointIndex == currentPath.getPointCount()) {
            currentPath.addPoint(startPoint);
        } else if (currentPointIndex == 0) {
            currentPath.setClosed(true);
        }

        redrawPath();
    }

    private Point moveCurrentPoint(MouseEvent ev) {
        Point2D dragPoint = new Point2D(ev.getX(), ev.getY());
        Point2D offset = dragPoint.subtract(startPoint);
        return currentPath.getPoint(currentPointIndex).add(offset);
    }

    public void mouseReleaseOnCanvas(MouseEvent ev) {
        if (startPoint != null && currentPointIndex < currentPath.getPointCount()) {
            currentPath.updatePoint(currentPointIndex, moveCurrentPoint(ev));
            redrawPath();
        }
    }

    public void mouseDragOnCanvas(MouseEvent ev) {
        if (startPoint != null && currentPointIndex < currentPath.getPointCount()) {
            Point origPoint = currentPath.getPoint(currentPointIndex);
            Point movedPoint = moveCurrentPoint(ev);
            currentPath.updatePoint(currentPointIndex, movedPoint);
            redrawPath();
            currentPath.updatePoint(currentPointIndex, origPoint);
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
        paths.clear();
        currentPath = new Path();
        paths.add(currentPath);
        startPoint = null;
        currentPointIndex = 0;
        redrawPath();
    }
}
