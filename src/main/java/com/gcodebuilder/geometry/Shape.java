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

package com.gcodebuilder.geometry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.gcodebuilder.app.GridSettings;
import com.gcodebuilder.app.tools.InteractionEvent;
import com.gcodebuilder.canvas.Drawable;
import com.gcodebuilder.changelog.Snapshot;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(Rectangle.class),
        @JsonSubTypes.Type(Circle.class),
        @JsonSubTypes.Type(Path.class),
        @JsonSubTypes.Type(Group.class)
})
@RequiredArgsConstructor
public abstract class Shape<H> implements Drawable {
    private static final Logger log = LogManager.getLogger(Shape.class);

    public static DataFormat SHAPE_REF_DATA_FORMAT = new DataFormat(
            "application/gcodebuilder+shape+ref");
    public static DataFormat SHAPE_JSON_DATA_FORMAT = new DataFormat(
            "application/json+gcodebuilder+shape");

    private final Class<H> handleClass;

    @Getter
    @Setter
    private int recipeId = 0;

    @Getter
    @Setter
    @JsonIgnore
    private boolean selected;

    private static AtomicLong refIdGenerator;
    static {
        // choose random initialId between 0 and Long.MAX_VALUE to ensure
        // generated refId values will almost never be equal to 0.
        long initialId = new SecureRandom().nextLong() & Long.MAX_VALUE;
        refIdGenerator = new AtomicLong(initialId);
    }

    @Getter
    @Setter
    @JsonIgnore
    private long refId = refIdGenerator.incrementAndGet();

    @JsonIgnore
    public String getRefIdAsString() {
        return Long.toUnsignedString(refId, 16);
    }

    public static long parseRefIdFromString(String refIdAsString) {
        try {
            return Long.parseUnsignedLong(refIdAsString, 16);
        } catch (NumberFormatException ex) {
            log.error("Failed to parse refId from string: {}", refIdAsString);
            return 0;
        }
    }

    public abstract H getHandle(Point2D point, Point2D mousePoint, double handleRadius);
    public abstract boolean edit(H handle, InteractionEvent event);

    public abstract boolean move(Point2D delta);

    @JsonIgnore
    public abstract Rectangle2D getBoundingBox();

    public abstract Point getCenter();

    public abstract boolean resize(double scaleFactor, Point center);

    public abstract List<Path> convertToPaths();

    public abstract Snapshot<? extends Shape<?>> save();

    public boolean castAndEdit(Object handle, InteractionEvent event) {
        return edit(handleClass.cast(handle), event);
    }

    protected void drawBoundingBox(GraphicsContext ctx, double pixelsPerUnit, GridSettings settings) {
        ctx.setStroke(settings.getBoundingBoxPaint());
        ctx.setLineWidth(settings.getBoundingBoxLineWidth() / pixelsPerUnit);
        Rectangle2D boundingBox = getBoundingBox();
        ctx.strokeRect(boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getWidth(), boundingBox.getHeight());
        Point center = getCenter();
        double centerOffset = settings.getBoundingBoxCenterOffset() / pixelsPerUnit;
        double centerMinX = center.getX() - centerOffset;
        double centerMaxX = center.getX() + centerOffset;
        double centerMinY = center.getY() - centerOffset;
        double centerMaxY = center.getY() + centerOffset;
        ctx.strokeLine(centerMinX, centerMinY, centerMaxX, centerMaxY);
        ctx.strokeLine(centerMinX, centerMaxY, centerMaxX, centerMinY);
    }

    protected void prepareToDraw(GraphicsContext ctx, double pixelsPerUnit, GridSettings settings) {
        if (isSelected()) {
            drawBoundingBox(ctx, pixelsPerUnit, settings);
            ctx.setStroke(settings.getSelectedShapePaint());
        } else {
            ctx.setStroke(settings.getShapePaint());
        }
        ctx.setFill(settings.getShapePaint());
        ctx.setLineWidth(settings.getShapeLineWidth() / pixelsPerUnit);
    }

    public void saveToClipboard(Clipboard clipboard) {
        ClipboardContent content = new ClipboardContent();
        content.put(SHAPE_REF_DATA_FORMAT, getRefIdAsString());
        ShapeIO.saveToClipboardContent(content, SHAPE_JSON_DATA_FORMAT, this);
        clipboard.setContent(content);
    }

    public static boolean clipboardHasContent(Clipboard clipboard, boolean followRefs) {
        return (followRefs && clipboard.hasContent(SHAPE_REF_DATA_FORMAT))
                || clipboard.hasContent(SHAPE_JSON_DATA_FORMAT);
    }

    public static Shape<?> loadFromClipboard(Clipboard clipboard, Drawing drawing, boolean followRefs) {
        if (followRefs && clipboard.hasContent(SHAPE_REF_DATA_FORMAT)) {
            long refId = parseRefIdFromString((String)clipboard.getContent(SHAPE_REF_DATA_FORMAT));
            for (Shape<?> shape : drawing.getShapes()) {
                if (refId == shape.getRefId()) {
                    return shape;
                }
            }
        }
        return ShapeIO.loadFromClipboard(clipboard, SHAPE_JSON_DATA_FORMAT, Shape.class);
    }
}
