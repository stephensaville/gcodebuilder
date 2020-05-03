package com.gcodebuilder.geometry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gcodebuilder.app.tools.InteractionEvent;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EqualsAndHashCode
public class Rectangle extends Shape<Rectangle.Handle> {
    private static final Logger log = LogManager.getLogger(Rectangle.class);

    public enum HandleType {
        BOTTOM_LEFT {
            public Rectangle2D move(Rectangle2D rect, InteractionEvent event) {
                double minX = Math.min(event.getPoint().getX(), rect.getMaxX());
                double minY = Math.min(event.getPoint().getY(), rect.getMaxY());
                return new Rectangle2D(minX, minY, rect.getMaxX() - minX, rect.getMaxY() - minY);
            }
        },
        LEFT {
            public Rectangle2D move(Rectangle2D rect, InteractionEvent event) {
                double minX = Math.min(event.getPoint().getX(), rect.getMaxX());
                return new Rectangle2D(minX, rect.getMinY(), rect.getMaxX() - minX, rect.getHeight());
            }
        },
        TOP_LEFT {
            public Rectangle2D move(Rectangle2D rect, InteractionEvent event) {
                double minX = Math.min(event.getPoint().getX(), rect.getMaxX());
                double maxY = Math.max(event.getPoint().getY(), rect.getMinY());
                return new Rectangle2D(minX, rect.getMinY(), rect.getMaxX() - minX, maxY - rect.getMinY());
            }
        },
        TOP {
            public Rectangle2D move(Rectangle2D rect, InteractionEvent event) {
                double maxY = Math.max(event.getPoint().getY(), rect.getMinY());
                return new Rectangle2D(rect.getMinX(), rect.getMinY(), rect.getWidth(), maxY - rect.getMinY());
            }
        },
        TOP_RIGHT {
            public Rectangle2D move(Rectangle2D rect, InteractionEvent event) {
                double maxX = Math.max(event.getPoint().getX(), rect.getMinX());
                double maxY = Math.max(event.getPoint().getY(), rect.getMinY());
                return new Rectangle2D(rect.getMinX(), rect.getMinY(), maxX - rect.getMinX(), maxY - rect.getMinY());
            }
        },
        RIGHT {
            public Rectangle2D move(Rectangle2D rect, InteractionEvent event) {
                double maxX = Math.max(event.getPoint().getX(), rect.getMinX());
                return new Rectangle2D(rect.getMinX(), rect.getMinY(), maxX - rect.getMinX(), rect.getHeight());
            }
        },
        BOTTOM_RIGHT {
            public Rectangle2D move(Rectangle2D rect, InteractionEvent event) {
                double maxX = Math.max(event.getPoint().getX(), rect.getMinX());
                double minY = Math.min(event.getPoint().getY(), rect.getMaxY());
                return new Rectangle2D(rect.getMinX(), minY, maxX - rect.getMinX(), rect.getMaxY() - minY);
            }
        },
        BOTTOM {
            public Rectangle2D move(Rectangle2D rect, InteractionEvent event) {
                double minY = Math.min(event.getPoint().getY(), rect.getMaxY());
                return new Rectangle2D(rect.getMinX(), minY, rect.getWidth(), rect.getMaxY() - minY);
            }
        };

        public abstract Rectangle2D move(Rectangle2D rect, InteractionEvent event);
    }

    @Data
    public class Handle {
        private final HandleType type;
        private final Rectangle2D original;
    }

    private Rectangle2D rect;

    @JsonCreator
    public Rectangle(@JsonProperty("minX") double minX,
                     @JsonProperty("minY") double minY,
                     @JsonProperty("width") double width,
                     @JsonProperty("height") double height) {
        this.rect = new Rectangle2D(minX, minY, width, height);
        log.debug("new {}", this);
    }

    public double getMinX() {
        return rect.getMinX();
    }

    public double getMinY() {
        return rect.getMinY();
    }

    public double getMaxX() {
        return rect.getMaxX();
    }

    public double getMaxY() {
        return rect.getMaxY();
    }

    public double getWidth() {
        return rect.getWidth();
    }

    public double getHeight() {
        return rect.getHeight();
    }

    public boolean update(Rectangle2D rect) {
        if (!rect.equals(this.rect)) {
            this.rect = rect;
            log.debug("update {}", this);
            return true;
        }
        return false;
    }

    public boolean update(double minX, double minY, double width, double height) {
        return update(new Rectangle2D(minX, minY, width, height));
    }

    @Override
    public Handle getHandle(Point2D point, Point2D mousePoint, double pixelsPerUnit) {
        if (rect == null) return null;
        double x = point.getX();
        double minX = getMinX();
        double maxX = getMaxX();
        double y = point.getY();
        double minY = getMinY();
        double maxY = getMaxY();
        HandleType type = null;
        if (x == minX) {
            if (y == minY) {
                type = HandleType.BOTTOM_LEFT;
            } else if (y == maxY) {
                type = HandleType.TOP_LEFT;
            } else if (y > minY && y < maxY) {
                type = HandleType.LEFT;
            }
        } else if (x == maxX) {
            if (y == minY) {
                type = HandleType.BOTTOM_RIGHT;
            } else if (y == maxY) {
                type = HandleType.TOP_RIGHT;
            } else if (y > minY && y < maxY) {
                type = HandleType.RIGHT;
            }
        } else if (y == minY && x > minX && x < maxX) {
            type = HandleType.BOTTOM;
        } else if (y == maxY && x > minX && x < maxX) {
            type = HandleType.TOP;
        }
        if (type != null) {
            return new Handle(type, rect);
        } else {
            return null;
        }
    }

    @Override
    public boolean edit(Handle handle, InteractionEvent event) {
        return update(handle.getType().move(rect, event));
    }

    @Override
    public boolean move(Handle handle, InteractionEvent event) {
        Point2D delta = event.getPoint().subtract(event.getStartPoint());
        Rectangle2D movedRect = new Rectangle2D(
                handle.getOriginal().getMinX() + delta.getX(),
                handle.getOriginal().getMinY() + delta.getY(),
                handle.getOriginal().getWidth(),
                handle.getOriginal().getHeight());
        return update(movedRect);
    }

    @Override
    public void draw(GraphicsContext ctx, double pixelsPerUnit) {
        if (rect != null) {
            log.debug("draw {}", this);
            ctx.setLineWidth(2 / pixelsPerUnit);
            ctx.setStroke(Color.BLACK);
            ctx.strokeRect(getMinX(), getMinY(), getWidth(), getHeight());
        }
    }

    @Override
    public String toString() {
        return String.format("Rectangle(minX=%f, minY=%f, width=%f, height=%f)",
                getMinX(), getMinY(), getWidth(), getHeight());
    }
}
