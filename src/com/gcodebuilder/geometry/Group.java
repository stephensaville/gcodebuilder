package com.gcodebuilder.geometry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.gcodebuilder.app.GridSettings;
import com.gcodebuilder.app.tools.InteractionEvent;
import com.gcodebuilder.canvas.Drawable;
import com.gcodebuilder.changelog.Snapshot;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@JsonTypeName("GROUP")
public class Group extends Shape<Group.Handle> {
    @Data
    public class Handle<H> {
        private final Shape<H> shape;
        private final H handle;
    }

    @Getter
    private final List<Shape<?>> shapes;

    @JsonCreator
    public Group(@JsonProperty("shapes") List<Shape<?>> shapes) {
        super(Handle.class);
        this.shapes = new ArrayList<>(shapes);
    }

    public Group() {
        super(Handle.class);
        this.shapes = new ArrayList<>();
    }

    public static Group groupSelected(Drawing drawing) {
        Group group = new Group();
        for (Shape<?> selectedShape : drawing.getSelectedShapes()) {
            drawing.remove(selectedShape);
            group.shapes.add(selectedShape);
        }
        if (group.isVisible()) {
            drawing.add(group);
        }
        return group;
    }

    public List<Shape<?>> ungroup(Drawing drawing) {
        drawing.remove(this);
        drawing.addAll(shapes);
        return shapes;
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        for (Shape<?> shape : shapes) {
            shape.setSelected(selected);
        }
    }

    private <H> Handle<H> getHandle(Shape<H> shape, Point2D point, Point2D mousePoint, double handleRadius) {
        H handle = shape.getHandle(point, mousePoint, handleRadius);
        return handle != null ? new Handle<>(shape, handle) : null;
    }

    @Override
    public Handle getHandle(Point2D point, Point2D mousePoint, double handleRadius) {
        for (Shape<?> shape : shapes) {
            if (shape instanceof Group) {
                return ((Group) shape).getHandle(shape, point, mousePoint, handleRadius);
            } else {
                Handle<?> handle = getHandle(shape, point, mousePoint, handleRadius);
                if (handle != null) {
                    return handle;
                }
            }
        }
        return null;
    }

    @Override
    public boolean edit(Handle handle, InteractionEvent event) {
        return handle.getShape().edit(handle.getHandle(), event);
    }

    @Override
    public boolean move(Point2D delta) {
        boolean updated = false;
        for (Shape<?> shape : shapes) {
            updated = shape.move(delta) || updated;
        }
        return updated;
    }

    @Override
    @JsonIgnore
    public Rectangle2D getBoundingBox() {
        return Math2D.computeBoundingBoxForShapes(shapes);
    }

    @Override
    @JsonIgnore
    public Point getCenter() {
        return new Point(Math2D.getBoundingBoxCenter(getBoundingBox()));
    }

    @Override
    public boolean resize(double scaleFactor, Point center) {
        boolean updated = false;
        for (Shape<?> shape : shapes) {
            updated = shape.resize(scaleFactor, center) || updated;
        }
        return updated;
    }

    @Override
    public Snapshot<Group> save() {
        return new Snapshot<>() {
            private final List<Snapshot<? extends Shape<?>>> savedShapes = Group.this.getShapes().stream()
                    .map(Shape::save).collect(Collectors.toList());

            @Override
            public Group restore() {
                shapes.clear();
                savedShapes.forEach(snapshot -> shapes.add(snapshot.restore()));
                return Group.this;
            }
        };
    }

    @Override
    public List<Path> convertToPaths() {
        return shapes.stream().flatMap(shape -> shape.convertToPaths().stream()).collect(Collectors.toList());
    }

    @Override
    public boolean isVisible() {
        return !shapes.isEmpty();
    }

    @Override
    public void draw(GraphicsContext ctx, double pixelsPerUnit, GridSettings settings) {
        for (Drawable shape : shapes) {
            shape.draw(ctx, pixelsPerUnit, settings);
        }
    }
}
