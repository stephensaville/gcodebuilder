package com.gcodebuilder.geometry;

import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Data
public class ShapeList {
    private static final DataFormat SHAPE_LIST_JSON_DATA_FORMAT = new DataFormat(
            "application/json+gcodebuilder+shape+list");

    private final List<Shape<?>> shapes;

    public ShapeList(Shape<?>... shapes) {
        this.shapes = new ArrayList<>(Arrays.asList(shapes));
    }

    public ShapeList(Collection<Shape<?>> shapes) {
        this.shapes = new ArrayList<>(shapes);
    }

    public void saveToClipboard(Clipboard clipboard) {
        ShapeIO.saveToClipboard(clipboard, SHAPE_LIST_JSON_DATA_FORMAT, this);
    }

    public static boolean clipboardHasContent(Clipboard clipboard) {
        return clipboard.hasContent(SHAPE_LIST_JSON_DATA_FORMAT);
    }

    public static ShapeList loadFromClipboard(Clipboard clipboard) {
        return ShapeIO.loadFromClipboard(clipboard, SHAPE_LIST_JSON_DATA_FORMAT, ShapeList.class);
    }
}
