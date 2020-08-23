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

    private int currentPointIndex = -1;

    @Override
    public Path down(InteractionEvent event) {
        Set<Shape<?>> selectedShapes = event.getDrawing().getSelectedShapes();
        Path currentPath = null;
        Point newPoint = new Point(event.getPoint());
        if (selectedShapes.size() == 1) {
            Shape<?> selectedShape = selectedShapes.iterator().next();
            if (selectedShape instanceof Path) {
                currentPath = (Path)selectedShape;
                if (currentPath.isClosed()) {
                    currentPath = null;
                } else if (newPoint.isSame(currentPath.getPoint(0), event.getHandleRadius())) {
                    currentPath.setClosed(true);
                    log.info("Closed path: {}", currentPath);
                    currentPointIndex = 0;
                    return currentPath;
                }
            }
        }
        if (currentPath == null) {
            currentPath = new Path();
            event.getDrawing().add(currentPath);
        }
        currentPointIndex = currentPath.getPointCount();
        currentPath.addPoint(new Point(event.getPoint()));
        return currentPath;
    }

    private void updateCurrentPath(InteractionEvent event) {
        Path currentPath = (Path)event.getShape();
        Point newPoint = new Point(event.getPoint());
        if (currentPath.updatePoint(currentPointIndex, newPoint)) {
            event.getDrawing().setDirty(true);
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
