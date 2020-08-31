package com.gcodebuilder.geometry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.gcodebuilder.app.GridSettings;
import com.gcodebuilder.app.tools.InteractionEvent;
import com.gcodebuilder.changelog.Snapshot;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@JsonTypeName("PATH_GROUP")
public class PathGroup extends Shape<PathGroup.Handle> {
    @Data
    public class Handle {
        private final Path path;
        private final Path.Handle handle;
    }

    @Getter
    private final List<Path> paths;

    @JsonCreator
    public PathGroup(@JsonProperty("paths") List<Path> paths) {
        super(Handle.class);
        this.paths = new ArrayList<>(paths);
    }

    public PathGroup() {
        super(Handle.class);
        this.paths = new ArrayList<>();
    }

    public static PathGroup groupSelected(Drawing drawing) {
        PathGroup pathGroup = new PathGroup();
        for (Path selectedPath : drawing.getSelectedShapes(Path.class)) {
            drawing.remove(selectedPath);
            pathGroup.paths.add(selectedPath);
        }
        for (PathGroup selectedGroup : drawing.getSelectedShapes(PathGroup.class)) {
            drawing.remove(selectedGroup);
            pathGroup.paths.addAll(selectedGroup.getPaths());
        }
        if (pathGroup.isVisible()) {
            drawing.add(pathGroup);
        }
        return pathGroup;
    }

    public List<Path> ungroup(Drawing drawing) {
        drawing.remove(this);
        drawing.addAll(paths);
        return paths;
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        for (Path path : paths) {
            path.setSelected(selected);
        }
    }

    @Override
    public Handle getHandle(Point2D point, Point2D mousePoint, double handleRadius) {
        for (Path path : paths) {
            Path.Handle pathHandle = path.getHandle(point, mousePoint, handleRadius);
            if (pathHandle != null) {
                return new Handle(path, pathHandle);
            }
        }
        return null;
    }

    @Override
    public boolean edit(Handle handle, InteractionEvent event) {
        return handle.getPath().edit(handle.getHandle(), event);
    }

    @Override
    public boolean move(Point2D delta) {
        boolean updated = false;
        for (Path path : paths) {
            updated = path.move(delta) || updated;
        }
        return updated;
    }

    @Override
    public Point getCenter() {
        Rectangle2D boundingBox = Math2D.computeBoundingBox(paths.stream()
                .flatMap(path -> path.getPoints().stream())
                .collect(Collectors.toList()));
        double centerX = boundingBox.getMinX() + boundingBox.getWidth() / 2;
        double centerY = boundingBox.getMinY() + boundingBox.getHeight() / 2;
        return new Point(centerX, centerY);
    }

    @Override
    public boolean resize(double scaleFactor, Point center) {
        boolean updated = false;
        for (Path path : paths) {
            updated = path.resize(scaleFactor, center) || updated;
        }
        return updated;
    }

    @Override
    public Snapshot<PathGroup> save() {
        return new Snapshot<>() {
            private final List<Snapshot<Path>> savedPaths = getPaths().stream()
                    .map(Path::save).collect(Collectors.toList());

            @Override
            public PathGroup restore() {
                paths.clear();
                savedPaths.forEach(pathSnapshot -> paths.add(pathSnapshot.restore()));
                return PathGroup.this;
            }
        };
    }

    @Override
    public boolean isVisible() {
        return !paths.isEmpty();
    }

    @Override
    public void draw(GraphicsContext ctx, double pixelsPerUnit, GridSettings settings) {
        for (Path path : paths) {
            path.draw(ctx, pixelsPerUnit, settings);
        }
    }
}
