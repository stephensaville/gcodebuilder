/*
 * Copyright (c) 2021 Stephen Saville
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gcodebuilder.app.tools;

import com.gcodebuilder.changelog.Snapshot;
import com.gcodebuilder.changelog.UpdateShapeChange;
import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Path;
import com.gcodebuilder.geometry.Group;
import com.gcodebuilder.geometry.Point;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.changelog.AddShapeChange;
import com.gcodebuilder.changelog.Change;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class PathTool implements Tool {
    private static final Logger log = LogManager.getLogger(Path.class);

    private Path.Handle currentHandle;
    private Snapshot<Path> pathBefore;

    private Path beginEdit(Path currentPath, InteractionEvent event) {
        pathBefore = currentPath.save();
        if (event.getInputEvent().getClickCount() > 1 && !currentHandle.isProjectedPoint()) {
            // double-click to remove point from path
            if (currentPath.removePoint(currentHandle.getPointIndex())) {
                event.getDrawing().setDirty(true);
            }
            log.info("Removed point: {} from path: {}", currentHandle.getOriginalPoint(), currentPath);
            currentHandle = null;
        } else if (currentHandle.getPointIndex() == 0 && !currentHandle.isProjectedPoint() && !currentPath.isClosed()) {
            currentPath.closePath();
            event.getDrawing().setDirty(true);
            log.info("Closed path: {}", currentPath);
        }
        return currentPath;
    }

    @Override
    public Path down(InteractionEvent event) {
        currentHandle = null;
        Path currentPath = event.getDrawing().getSelectedShape(Path.class);
        if (currentPath != null) {
            currentHandle = currentPath.getHandle(event.getPoint(), event.getMousePoint(), event.getHandleRadius());
        }
        Group currentGroup = event.getDrawing().getSelectedShape(Group.class);
        if (currentGroup != null) {
            Group.Handle<?> groupHandle = currentGroup.getHandle(
                    event.getPoint(), event.getMousePoint(), event.getHandleRadius());
            if (groupHandle != null && groupHandle.getShape() instanceof Path) {
                currentPath = (Path)groupHandle.getShape();
                currentHandle = (Path.Handle)groupHandle.getHandle();
            }
        }
        if (currentHandle == null) {
            if (event.getShape() instanceof Path) {
                currentPath = (Path) event.getShape();
                currentHandle = (Path.Handle) event.getHandle();
            } else if (event.getShape() instanceof Group) {
                Group.Handle<?> groupHandle = (Group.Handle)event.getHandle();
                if (groupHandle != null && groupHandle.getShape() instanceof Path) {
                    currentPath = (Path) groupHandle.getShape();
                    currentHandle = (Path.Handle) groupHandle.getHandle();
                }
            }
        }
        if (currentHandle != null) {
            return beginEdit(currentPath, event);
        }
        if (currentPath == null || currentPath.isClosed()) {
            currentPath = new Path();
            pathBefore = null;
            event.getDrawing().add(currentPath);
            log.info("Created new path.");
        } else {
            pathBefore = currentPath.save();
        }

        if (!currentPath.isClosed() && currentPath.getPointCount() > 0 &&
                currentPath.getPoint(0).isSame(event.getPoint())) {
            currentPath.closePath();
            event.getDrawing().setDirty(true);
            log.info("Closed path: {}", currentPath);
        } else {
            int newPointIndex = currentPath.getPointCount();
            Point.Type pointType = null;
            if (event.getInputEvent().isControlDown()) {
                if (event.getInputEvent().isShiftDown()) {
                    pointType = Point.Type.CCW_CENTER;
                } else {
                    pointType = Point.Type.CW_CENTER;
                }
            }
            Point newPoint = new Point(event.getPoint(), pointType);
            currentPath.addPoint(newPoint);
            currentHandle = currentPath.getHandle(newPointIndex);
            event.getDrawing().setDirty(true);
            log.info("Added new point: {} to path: {}", newPoint, currentPath);
        }
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
        if (pathBefore == null) {
            return () -> new AddShapeChange("Add Path", drawing, shape.save());
        } else {
            final Snapshot<Path> before = pathBefore;
            return () -> new UpdateShapeChange("Edit Path", drawing, before, shape.save());
        }
    }
}
