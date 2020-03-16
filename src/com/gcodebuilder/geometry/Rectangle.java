package com.gcodebuilder.geometry;

import com.gcodebuilder.app.tools.RectangleTool;
import com.gcodebuilder.app.tools.Tool;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Rectangle extends Shape {
    private static final Logger log = LogManager.getLogger(Rectangle.class);

    @Getter
    private Rectangle2D rect;

    public Rectangle(Rectangle2D rect) {
        this.rect = rect;
        log.info("Rectangle created: {}", rect);
    }

    public Rectangle(double x, double y, double width, double height) {
        this(new Rectangle2D(x, y, width, height));
    }

    public boolean updateRect(Rectangle2D rect) {
        if (!rect.equals(this.rect)) {
            this.rect = rect;
            log.info("Rectangle updated: {}", rect);
            return true;
        }
        return false;
    }

    @Override
    public Tool getEditingTool() {
        return RectangleTool.instance();
    }

    @Override
    public void draw(GraphicsContext ctx, double pixelsPerUnit) {
        if (rect != null) {
            log.info("Drawing rectangle: {}", rect);
            ctx.setLineWidth(2 / pixelsPerUnit);
            ctx.setStroke(Color.BLACK);
            ctx.strokeRect(rect.getMinX(), rect.getMinY(), rect.getWidth(), rect.getHeight());
        }
    }
}
