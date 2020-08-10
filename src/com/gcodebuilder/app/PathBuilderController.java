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
    @FXML private BorderPane rootPane;
    @FXML private Canvas pathCanvas;

    private GraphicsContext ctx;
    private ToolpathGenerator generator = new ToolpathGenerator();
    private List<Path> paths = new ArrayList<>();
    private Path currentPath;
    private Path.Handle currentHandle;
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
        Point2D mousePoint = new Point2D(ev.getX(), ev.getY());

        currentHandle = null;
        for (Path path : paths) {
            currentHandle = path.getHandle(mousePoint, mousePoint, generator.getPointRadius());
            if (currentHandle != null) {
                currentPath = path;
                break;
            }
        }

        if (currentHandle == null) {
            currentPath = paths.get(paths.size() - 1);
            int newPointIndex = currentPath.getPointCount();
            currentPath.addPoint(mousePoint);
            currentHandle = currentPath.getHandle(newPointIndex);
        } else if (!currentPath.isClosed() && currentHandle.getPointIndex() == 0 && currentPath.getPointCount() > 2) {
            currentPath.setClosed(true);

            Path newPath = new Path();
            paths.add(newPath);
            generator.addPath(newPath);
        }

        redrawPath();
    }

    public void mouseReleaseOnCanvas(MouseEvent ev) {
        if (currentHandle != null) {
            currentPath.updatePoint(currentHandle.getPointIndex(), new Point(ev.getX(), ev.getY()));
            redrawPath();
        }
    }

    public void mouseDragOnCanvas(MouseEvent ev) {
        if (currentHandle != null) {
            currentPath.updatePoint(currentHandle.getPointIndex(), new Point(ev.getX(), ev.getY()));
            redrawPath();
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
        generator.clearPaths();
        currentPath = new Path();
        paths.add(currentPath);
        generator.addPath(currentPath);
        currentHandle = null;
        redrawPath();
    }
}
