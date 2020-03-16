package com.gcodebuilder.geometry;

import com.gcodebuilder.app.tools.InteractionEvent;
import com.gcodebuilder.app.tools.RectangleTool;
import com.gcodebuilder.app.tools.Tool;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Rectangle extends Shape {
    private static final Logger log = LogManager.getLogger(Rectangle.class);

    public enum Handle {
        DEFAULT {
            public Rectangle2D move(Rectangle2D rect, InteractionEvent event) {
                return new Rectangle2D(
                        Math.min(event.getX(), event.getStartX()),
                        Math.min(event.getY(), event.getStartY()),
                        Math.abs(event.getX() - event.getStartX()),
                        Math.abs(event.getY() - event.getStartY()));
            }
        },
        BOTTOM_LEFT {
            public Rectangle2D move(Rectangle2D rect, InteractionEvent event) {
                double minX = Math.min(event.getX(), rect.getMaxX());
                double minY = Math.min(event.getY(), rect.getMaxY());
                return new Rectangle2D(minX, minY, rect.getMaxX() - minX, rect.getMaxY() - minY);
            }
        },
        LEFT {
            public Rectangle2D move(Rectangle2D rect, InteractionEvent event) {
                double minX = Math.min(event.getX(), rect.getMaxX());
                return new Rectangle2D(minX, rect.getMinY(), rect.getMaxX() - minX, rect.getHeight());
            }
        },
        TOP_LEFT {
            public Rectangle2D move(Rectangle2D rect, InteractionEvent event) {
                double minX = Math.min(event.getX(), rect.getMaxX());
                double maxY = Math.max(event.getY(), rect.getMinY());
                return new Rectangle2D(minX, rect.getMinY(), rect.getMaxX() - minX, maxY - rect.getMinY());
            }
        },
        TOP {
            public Rectangle2D move(Rectangle2D rect, InteractionEvent event) {
                double maxY = Math.max(event.getY(), rect.getMinY());
                return new Rectangle2D(rect.getMinX(), rect.getMinY(), rect.getWidth(), maxY - rect.getMinY());
            }
        },
        TOP_RIGHT {
            public Rectangle2D move(Rectangle2D rect, InteractionEvent event) {
                double maxX = Math.max(event.getX(), rect.getMinX());
                double maxY = Math.max(event.getY(), rect.getMinY());
                return new Rectangle2D(rect.getMinX(), rect.getMinY(), maxX - rect.getMinX(), maxY - rect.getMinY());
            }
        },
        RIGHT {
            public Rectangle2D move(Rectangle2D rect, InteractionEvent event) {
                double maxX = Math.max(event.getX(), rect.getMinX());
                return new Rectangle2D(rect.getMinX(), rect.getMinY(), maxX - rect.getMinX(), rect.getHeight());
            }
        },
        BOTTOM_RIGHT {
            public Rectangle2D move(Rectangle2D rect, InteractionEvent event) {
                double maxX = Math.max(event.getX(), rect.getMinX());
                double minY = Math.min(event.getY(), rect.getMaxY());
                return new Rectangle2D(rect.getMinX(), minY, maxX - rect.getMinX(), rect.getMaxY() - minY);
            }
        },
        BOTTOM {
            public Rectangle2D move(Rectangle2D rect, InteractionEvent event) {
                double minY = Math.min(event.getY(), rect.getMaxY());
                return new Rectangle2D(rect.getMinX(), minY, rect.getWidth(), rect.getMaxY() - minY);
            }
        };

        public abstract Rectangle2D move(Rectangle2D rect, InteractionEvent event);
    }

    @Getter
    private Rectangle2D rect;

    public Rectangle(Rectangle2D rect) {
        this.rect = rect;
        log.info("Rectangle created: {}", rect);
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
    public Handle getHandle(Point2D gridPoint) {
        if (rect == null) return null;
        double x = gridPoint.getX();
        double minX = rect.getMinX();
        double maxX = rect.getMaxX();
        double y = gridPoint.getY();
        double minY = rect.getMinY();
        double maxY = rect.getMaxY();
        if (x == minX) {
            if (y == minY) {
                return Handle.BOTTOM_LEFT;
            } else if (y == maxY) {
                return Handle.TOP_LEFT;
            } else if (y > minY && y < maxY) {
                return Handle.LEFT;
            }
        } else if (x == maxX) {
            if (y == minY) {
                return Handle.BOTTOM_RIGHT;
            } else if (y == maxY) {
                return Handle.TOP_RIGHT;
            } else if (y > minY && y < maxY) {
                return Handle.RIGHT;
            }
        } else if (y == minY && x > minX && x < maxX) {
            return Handle.BOTTOM;
        } else if (y == maxY && x > minX && x < maxX) {
            return Handle.TOP;
        }
        return null;
    }

    @Override
    public Enum<?> getDefaultHandle() {
        return Handle.DEFAULT;
    }

    @Override
    public boolean moveHandle(Enum<?> handle, InteractionEvent event) {
        return updateRect(((Handle)handle).move(rect, event));
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
