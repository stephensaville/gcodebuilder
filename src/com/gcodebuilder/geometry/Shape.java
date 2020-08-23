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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, property="type")
@JsonSubTypes({
        @JsonSubTypes.Type(Rectangle.class),
        @JsonSubTypes.Type(Circle.class),
        @JsonSubTypes.Type(Path.class)
})
@RequiredArgsConstructor
public abstract class Shape<H> implements Drawable {
    private static final Logger log = LogManager.getLogger(Shape.class);

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
}
