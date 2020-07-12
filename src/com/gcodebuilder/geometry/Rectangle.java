package com.gcodebuilder.geometry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gcodebuilder.app.GridSettings;
import com.gcodebuilder.app.tools.InteractionEvent;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EqualsAndHashCode
public class Rectangle extends Shape<Rectangle.Handle> {
    private static final Logger log = LogManager.getLogger(Rectangle.class);

    public enum HandleType {
        BOTTOM_LEFT {
            public boolean move(Rectangle rect, InteractionEvent event) {
                double minX = Math.min(event.getPoint().getX(), rect.getMaxX());
                double minY = Math.min(event.getPoint().getY(), rect.getMaxY());
                return rect.update(minX, minY, rect.getMaxX() - minX, rect.getMaxY() - minY);
            }
        },
        LEFT {
            public boolean move(Rectangle rect, InteractionEvent event) {
                double minX = Math.min(event.getPoint().getX(), rect.getMaxX());
                return rect.update(minX, rect.getMinY(), rect.getMaxX() - minX, rect.getHeight());
            }
        },
        TOP_LEFT {
            public boolean move(Rectangle rect, InteractionEvent event) {
                double minX = Math.min(event.getPoint().getX(), rect.getMaxX());
                double maxY = Math.max(event.getPoint().getY(), rect.getMinY());
                return rect.update(minX, rect.getMinY(), rect.getMaxX() - minX, maxY - rect.getMinY());
            }
        },
        TOP {
            public boolean move(Rectangle rect, InteractionEvent event) {
                double maxY = Math.max(event.getPoint().getY(), rect.getMinY());
                return rect.update(rect.getMinX(), rect.getMinY(), rect.getWidth(), maxY - rect.getMinY());
            }
        },
        TOP_RIGHT {
            public boolean move(Rectangle rect, InteractionEvent event) {
                double maxX = Math.max(event.getPoint().getX(), rect.getMinX());
                double maxY = Math.max(event.getPoint().getY(), rect.getMinY());
                return rect.update(rect.getMinX(), rect.getMinY(), maxX - rect.getMinX(), maxY - rect.getMinY());
            }
        },
        RIGHT {
            public boolean move(Rectangle rect, InteractionEvent event) {
                double maxX = Math.max(event.getPoint().getX(), rect.getMinX());
                return rect.update(rect.getMinX(), rect.getMinY(), maxX - rect.getMinX(), rect.getHeight());
            }
        },
        BOTTOM_RIGHT {
            public boolean move(Rectangle rect, InteractionEvent event) {
                double maxX = Math.max(event.getPoint().getX(), rect.getMinX());
                double minY = Math.min(event.getPoint().getY(), rect.getMaxY());
                return rect.update(rect.getMinX(), minY, maxX - rect.getMinX(), rect.getMaxY() - minY);
            }
        },
        BOTTOM {
            public boolean move(Rectangle rect, InteractionEvent event) {
                double minY = Math.min(event.getPoint().getY(), rect.getMaxY());
                return rect.update(rect.getMinX(), minY, rect.getWidth(), rect.getMaxY() - minY);
            }
        };

        public abstract boolean move(Rectangle rect, InteractionEvent event);
    }

    @Data
    public class Handle {
        private final HandleType type;
        private final double originalMinX;
        private final double originalMinY;
    }

    @Getter
    private double minX;

    @Getter
    private double minY;

    @Getter
    private double width;

    @Getter
    private double height;

    @JsonCreator
    public Rectangle(@JsonProperty("minX") double minX,
                     @JsonProperty("minY") double minY,
                     @JsonProperty("width") double width,
                     @JsonProperty("height") double height) {
        this.minX = minX;
        this.minY = minY;
        this.width = width;
        this.height = height;
        log.debug("new {}", this);
    }

    @JsonIgnore
    public double getMaxX() {
        return minX + width;
    }

    @JsonIgnore
    public double getMaxY() {
        return minY + height;
    }

    public boolean updateMinX(double newMinX) {
        if (minX != newMinX) {
            minX = newMinX;
            return true;
        }
        return false;
    }

    public boolean updateMinY(double newMinY) {
        if (minY != newMinY) {
            minY = newMinY;
            return true;
        }
        return false;
    }

    public boolean updatePosition(double newMinX, double newMinY) {
        boolean updated = updateMinX(newMinX);
        updated = updateMinY(newMinY) || updated;
        return updated;
    }

    public boolean updateWidth(double newWidth) {
        if (width != newWidth) {
            width = newWidth;
            return true;
        }
        return false;
    }

    public boolean updateHeight(double newHeight) {
        if (height != newHeight) {
            height = newHeight;
            return true;
        }
        return false;
    }

    public boolean update(double newMinX, double newMinY, double newWidth, double newHeight) {
        boolean updated = updateMinX(newMinX);
        updated = updateMinY(newMinY) || updated;
        updated = updateWidth(newWidth) || updated;
        updated = updateHeight(newHeight) || updated;
        return updated;
    }

    @Override
    public Handle getHandle(Point2D point, Point2D mousePoint, double pixelsPerUnit) {
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
            return new Handle(type, minX, minY);
        } else {
            return null;
        }
    }

    @Override
    public boolean edit(Handle handle, InteractionEvent event) {
        return handle.getType().move(this, event);
    }

    @Override
    public boolean move(Handle handle, InteractionEvent event) {
        Point2D delta = event.getPoint().subtract(event.getStartPoint());
        return updatePosition(
                handle.getOriginalMinX() + delta.getX(),
                handle.getOriginalMinY() + delta.getY());
    }

    @Override
    public boolean isVisible() {
        return getWidth() > 0 || getHeight() > 0;
    }

    @Override
    public void draw(GraphicsContext ctx, double pixelsPerUnit, GridSettings settings) {
        prepareToDraw(ctx, pixelsPerUnit, settings);
        ctx.strokeRect(getMinX(), getMinY(), getWidth(), getHeight());
    }

    @Override
    public String toString() {
        return String.format("Rectangle(minX=%f, minY=%f, width=%f, height=%f)",
                getMinX(), getMinY(), getWidth(), getHeight());
    }
}
