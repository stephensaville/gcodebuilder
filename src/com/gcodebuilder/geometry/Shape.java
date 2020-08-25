package com.gcodebuilder.geometry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.gcodebuilder.app.GridSettings;
import com.gcodebuilder.app.tools.InteractionEvent;
import com.gcodebuilder.canvas.Drawable;
import com.gcodebuilder.changelog.Snapshot;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(Rectangle.class),
        @JsonSubTypes.Type(Circle.class),
        @JsonSubTypes.Type(Path.class)
})
@RequiredArgsConstructor
public abstract class Shape<H> implements Drawable {
    private static final Logger log = LogManager.getLogger(Shape.class);

    public static DataFormat SHAPE_REF_DATA_FORMAT = new DataFormat(
            "application/" + Shape.class.getName() + ".ref");
    public static DataFormat SHAPE_JSON_DATA_FORMAT = new DataFormat(
            "application/json");

    private final Class<H> handleClass;

    @Getter
    @Setter
    private int recipeId = 0;

    @Getter
    @Setter
    @JsonIgnore
    private boolean selected;

    public abstract H getHandle(Point2D point, Point2D mousePoint, double handleRadius);
    public abstract boolean edit(H handle, InteractionEvent event);
    public abstract boolean move(H handle, InteractionEvent event);
    public abstract boolean resize(H handle, InteractionEvent event);
    public abstract Snapshot<? extends Shape<?>> save();

    public boolean castAndEdit(Object handle, InteractionEvent event) {
        return edit(handleClass.cast(handle), event);
    }

    public boolean castAndMove(Object handle, InteractionEvent event) {
        return move(handleClass.cast(handle), event);
    }

    public boolean castAndResize(Object handle, InteractionEvent event) {
        return resize(handleClass.cast(handle), event);
    }

    protected void prepareToDraw(GraphicsContext ctx, double pixelsPerUnit, GridSettings settings) {
        ctx.setLineWidth(settings.getShapeLineWidth() / pixelsPerUnit);
        if (isSelected()) {
            ctx.setStroke(settings.getSelectedShapePaint());
        } else {
            ctx.setStroke(settings.getShapePaint());
        }
    }

    @JsonIgnore
    public String getRefId() {
        return Integer.toHexString(System.identityHashCode(this));
    }

    @JsonIgnore
    public ClipboardContent getClipboardContent() {
        ClipboardContent content = new ClipboardContent();
        content.put(SHAPE_REF_DATA_FORMAT, getRefId());
        try {
            content.put(SHAPE_JSON_DATA_FORMAT, Drawing.saveAsString(this));
        } catch (IOException ex) {
            log.error("Failed to serialize shape to JSON.", ex);
        }
        return content;
    }

    public static boolean clipboardHasShapeContent(Clipboard clipboard) {
        return clipboard.hasContent(SHAPE_REF_DATA_FORMAT) || clipboard.hasContent(SHAPE_JSON_DATA_FORMAT);
    }

    public static Shape<?> getShapeFromClipboard(Clipboard clipboard, Drawing drawing) {
        try {
            if (clipboard.hasContent(SHAPE_REF_DATA_FORMAT)) {
                String refId = (String) clipboard.getContent(SHAPE_REF_DATA_FORMAT);
                for (Shape<?> shape : drawing.getShapes()) {
                    if (refId.equals(shape.getRefId())) {
                        return shape;
                    }
                }
            }
            if (clipboard.hasContent(SHAPE_JSON_DATA_FORMAT)) {
                String shapeJSON = (String)clipboard.getContent(SHAPE_JSON_DATA_FORMAT);
                try {
                    return Drawing.loadFromString(shapeJSON, Shape.class);
                } catch (IOException ex) {
                    log.error("Failed to parse shape JSON from clipboard.", ex);
                }
            }
        } catch (Exception ex) {
            log.error("Failed to get shape from clipboard.", ex);
        }
        return null;
    }
}
