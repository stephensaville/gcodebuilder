package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Path;
import com.gcodebuilder.geometry.Point;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.changelog.AddShapeChange;
import com.gcodebuilder.changelog.Change;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.function.Supplier;

public class PathTool implements Tool {
    private static final Logger log = LogManager.getLogger(Path.class);

    private Path.Handle currentHandle;

    private Path getSelectedPath(InteractionEvent event) {
        Set<Shape<?>> selectedShapes = event.getDrawing().getSelectedShapes();
        if (selectedShapes.size() == 1) {
            Shape<?> selectedShape = selectedShapes.iterator().next();
            if (selectedShape instanceof Path) {
                return (Path) selectedShape;
            }
        }
        return null;
    }

    @Override
    public Path down(InteractionEvent event) {
        Point newPoint = new Point(event.getPoint());
        Path currentPath = getSelectedPath(event);
        if (currentPath != null) {
            currentHandle = currentPath.getHandle(event.getPoint(), event.getMousePoint(), event.getHandleRadius());
            if (currentHandle != null) {
                if (event.getInputEvent().getClickCount() > 1 && !currentHandle.isProjectedPoint()) {
                    // double-click to remove point from path
                    if (currentPath.removePoint(currentHandle.getPointIndex())) {
                        event.getDrawing().setDirty(true);
                    }
                    currentHandle = null;
                    return currentPath;
                }
                log.info("Editing point or segment in currently selected path.");
                if (currentHandle.getPointIndex() == 0 && !currentHandle.isProjectedPoint() && !currentPath.isClosed()) {
                    currentPath.closePath();
                    event.getDrawing().setDirty(true);
                    log.info("Closed path: " + currentPath);
                }
                return currentPath;
            }
        }
        if (currentHandle == null && (event.getShape() instanceof Path)) {
            currentPath = (Path) event.getShape();
            currentHandle = (Path.Handle) event.getHandle();
            log.info("Switched to editing point or segment in a different path: {}", currentPath);
            return currentPath;
        }
        if (currentPath == null || currentPath.isClosed()) {
            currentPath = new Path();
            event.getDrawing().add(currentPath);
            log.info("Created new path.");
        }
        int newPointIndex = currentPath.getPointCount();
        currentPath.addPoint(newPoint);
        currentHandle = currentPath.getHandle(newPointIndex);
        event.getDrawing().setDirty(true);
        log.info("Added new point: {} to path: {}", newPoint, currentPath);
        return currentPath;
    }

    private void updateCurrentPath(InteractionEvent event) {
        if (currentHandle != null) {
            Path currentPath = (Path) event.getShape();
            if (currentPath.edit(currentHandle, event)) {
                event.getDrawing().setDirty(true);
            }
        }
    }

    @Override
    public void drag(InteractionEvent event) {
        updateCurrentPath(event);
    }

    @Override
    public void up(InteractionEvent event) {
        updateCurrentPath(event);
    }

    @Override
    public Supplier<Change> prepareChange(Drawing drawing, Shape<?> shape) {
        return () -> new AddShapeChange("Path", drawing, shape.save());
    }
}
