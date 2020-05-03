package com.gcodebuilder.geometry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcodebuilder.canvas.Drawable;
import javafx.scene.canvas.GraphicsContext;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Drawing implements Drawable {
    private static final ObjectMapper OM = new ObjectMapper();

    @Getter
    private final List<Shape> shapes = new ArrayList<>();

    @Getter
    @Setter
    @JsonIgnore
    private boolean dirty = true;

    public void add(Shape shape) {
        shapes.add(shape);
        dirty = true;
    }

    public boolean remove(Shape shape) {
        if (shapes.remove(shape)) {
            dirty = true;
            return true;
        }
        return false;
    }

    @Override
    public void draw(GraphicsContext ctx, double pixelsPerUnit) {
        for (Drawable shape : shapes) {
            shape.draw(ctx, pixelsPerUnit);
        }
        dirty = false;
    }

    public void save(OutputStream out) throws IOException {
        OM.writeValue(out, this);
    }

    public String saveAsString() throws IOException {
        return OM.writeValueAsString(this);
    }

    public static Drawing load(InputStream in) throws IOException {
        return OM.readValue(in, Drawing.class);
    }

    public static Drawing load(String saved) throws IOException {
        return OM.readValue(saved, Drawing.class);
    }

    @Override
    public String toString() {
        return String.format("Drawing(shapes=%s, dirty=%s)", shapes, dirty);
    }
}
