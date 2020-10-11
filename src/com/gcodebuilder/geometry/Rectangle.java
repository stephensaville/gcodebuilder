package com.gcodebuilder.geometry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.gcodebuilder.app.GridSettings;
import com.gcodebuilder.app.tools.InteractionEvent;
import com.gcodebuilder.changelog.Snapshot;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import lombok.Data;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@JsonTypeName("RECTANGLE")
public class Rectangle extends SimpleShape<Rectangle.Handle> {
    private static final Logger log = LogManager.getLogger(Rectangle.class);

    private static double scaleFactorMin(double originalMin, double eventValue, double span) {
        double eventMin = Math.min(eventValue, originalMin + span / 2.0);
        return 1.0 + 2.0 * (originalMin - eventMin) / span;
    }

    private static double scaleFactorMax(double originalMax, double eventValue, double span) {
        double eventMax = Math.max(eventValue, originalMax - span / 2.0);
        return 1.0 + 2.0 * (eventMax - originalMax) / span;
    }

    public enum HandleType {
        BOTTOM_LEFT {
            public boolean move(Rectangle rect, InteractionEvent event) {
                double minX = Math.min(event.getPoint().getX(), rect.getMaxX());
                double minY = Math.min(event.getPoint().getY(), rect.getMaxY());
                return rect.update(minX, minY, rect.getMaxX() - minX, rect.getMaxY() - minY);
            }

            @Override
            public double scale(Rectangle rect, InteractionEvent event) {
                return Math.max(
                    scaleFactorMin(rect.getMinX(), event.getPoint().getX(), rect.getWidth()),
                    scaleFactorMin(rect.getMinY(), event.getPoint().getY(), rect.getHeight()));
            }
        },
        LEFT {
            public boolean move(Rectangle rect, InteractionEvent event) {
                double minX = Math.min(event.getPoint().getX(), rect.getMaxX());
                return rect.update(minX, rect.getMinY(), rect.getMaxX() - minX, rect.getHeight());
            }

            @Override
            public double scale(Rectangle rect, InteractionEvent event) {
                return scaleFactorMin(rect.getMinX(), event.getPoint().getX(), rect.getWidth());
            }
        },
        TOP_LEFT {
            public boolean move(Rectangle rect, InteractionEvent event) {
                double minX = Math.min(event.getPoint().getX(), rect.getMaxX());
                double maxY = Math.max(event.getPoint().getY(), rect.getMinY());
                return rect.update(minX, rect.getMinY(), rect.getMaxX() - minX, maxY - rect.getMinY());
            }

            @Override
            public double scale(Rectangle rect, InteractionEvent event) {
                return Math.max(
                        scaleFactorMin(rect.getMinX(), event.getPoint().getX(), rect.getWidth()),
                        scaleFactorMax(rect.getMaxY(), event.getPoint().getY(), rect.getHeight()));
            }
        },
        TOP {
            public boolean move(Rectangle rect, InteractionEvent event) {
                double maxY = Math.max(event.getPoint().getY(), rect.getMinY());
                return rect.update(rect.getMinX(), rect.getMinY(), rect.getWidth(), maxY - rect.getMinY());
            }

            @Override
            public double scale(Rectangle rect, InteractionEvent event) {
                return scaleFactorMax(rect.getMaxY(), event.getPoint().getY(), rect.getHeight());
            }
        },
        TOP_RIGHT {
            public boolean move(Rectangle rect, InteractionEvent event) {
                double maxX = Math.max(event.getPoint().getX(), rect.getMinX());
                double maxY = Math.max(event.getPoint().getY(), rect.getMinY());
                return rect.update(rect.getMinX(), rect.getMinY(), maxX - rect.getMinX(), maxY - rect.getMinY());
            }

            @Override
            public double scale(Rectangle rect, InteractionEvent event) {
                return Math.max(
                        scaleFactorMax(rect.getMaxX(), event.getPoint().getX(), rect.getWidth()),
                        scaleFactorMax(rect.getMaxY(), event.getPoint().getY(), rect.getHeight()));
            }
        },
        RIGHT {
            public boolean move(Rectangle rect, InteractionEvent event) {
                double maxX = Math.max(event.getPoint().getX(), rect.getMinX());
                return rect.update(rect.getMinX(), rect.getMinY(), maxX - rect.getMinX(), rect.getHeight());
            }

            @Override
            public double scale(Rectangle rect, InteractionEvent event) {
                return scaleFactorMax(rect.getMaxX(), event.getPoint().getX(), rect.getWidth());
            }
        },
        BOTTOM_RIGHT {
            public boolean move(Rectangle rect, InteractionEvent event) {
                double maxX = Math.max(event.getPoint().getX(), rect.getMinX());
                double minY = Math.min(event.getPoint().getY(), rect.getMaxY());
                return rect.update(rect.getMinX(), minY, maxX - rect.getMinX(), rect.getMaxY() - minY);
            }

            @Override
            public double scale(Rectangle rect, InteractionEvent event) {
                return Math.max(
                        scaleFactorMax(rect.getMaxX(), event.getPoint().getX(), rect.getWidth()),
                        scaleFactorMin(rect.getMinY(), event.getPoint().getY(), rect.getHeight()));
            }
        },
        BOTTOM {
            public boolean move(Rectangle rect, InteractionEvent event) {
                double minY = Math.min(event.getPoint().getY(), rect.getMaxY());
                return rect.update(rect.getMinX(), minY, rect.getWidth(), rect.getMaxY() - minY);
            }

            @Override
            public double scale(Rectangle rect, InteractionEvent event) {
                return scaleFactorMin(rect.getMinY(), event.getPoint().getY(), rect.getHeight());
            }
        };

        public abstract boolean move(Rectangle rect, InteractionEvent event);
        public abstract double scale(Rectangle rect, InteractionEvent event);
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
        super(Handle.class);
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

    public boolean scale(double scaleFactor) {
        if (scaleFactor != 1.0) {
            double newWidth = getWidth() * scaleFactor;
            double newHeight = getHeight() * scaleFactor;
            double newMinX = minX + (getWidth() - newWidth) / 2;
            double newMinY = minY + (getHeight() - newHeight) / 2;
            return update(newMinX, newMinY, newWidth, newHeight);
        } else {
            return false;
        }
    }

    @Override
    public Handle getHandle(Point2D point, Point2D mousePoint, double handleRadius) {
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
    public boolean move(Point2D delta) {
        return updatePosition(getMinX() + delta.getX(), getMinY() + delta.getY());
    }

    @Override
    public Rectangle2D getBoundingBox() {
        return new Rectangle2D(minX, minY, width, height);
    }

    @Override
    public Point getCenter() {
        return new Point(getMinX() + getWidth() / 2, getMinY() + getHeight() / 2);
    }

    @Override
    public boolean resize(double scaleFactor, Point center) {
        double newWidth = getWidth() * scaleFactor;
        double newHeight = getHeight() * scaleFactor;
        return update(center.getX() - newWidth / 2,
                center.getY() - newHeight / 2,
                newWidth, newHeight);
    }

    @Override
    public Snapshot<Rectangle> save() {
        return new Snapshot<>() {
            private final double minX = getMinX();
            private final double minY = getMinY();
            private final double width = getWidth();
            private final double height = getHeight();

            @Override
            public Rectangle restore() {
                update(minX, minY, width, height);
                return Rectangle.this;
            }
        };
    }

    @Override
    public Path convertToPath() {
        Path path = new Path();
        path.addPoint(getMinX(), getMinY());
        path.addPoint(getMinX(), getMaxY());
        path.addPoint(getMaxX(), getMaxY());
        path.addPoint(getMaxX(), getMinY());
        path.closePath();
        return path;
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
