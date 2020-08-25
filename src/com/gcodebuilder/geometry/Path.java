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
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonTypeName("PATH")
public class Path extends Shape<Path.Handle> {
    private static final Logger log = LogManager.getLogger(Path.class);

    private List<Point> points;
    private List<Segment> segments;

    @Getter @Setter
    private boolean closed;

    @JsonCreator
    public Path(@JsonProperty("points") List<Point> points,
                @JsonProperty("closed") boolean closed) {
        super(Handle.class);
        this.points = new ArrayList<>(points);
        this.closed = closed;
    }

    public Path() {
        super(Handle.class);
        this.points = new ArrayList<>();
        this.closed = false;
    }

    public List<Point> getPoints() {
        return Collections.unmodifiableList(points);
    }

    @JsonIgnore
    public List<Segment> getSegments() {
        if (points.size() < 2) {
            return Collections.emptyList();
        }
        if (segments == null) {
            segments = new ArrayList<>();
            Point prevPoint = closed ? points.get(points.size() - 1) : null;
            for (Point currentPoint : points) {
                if (prevPoint != null) {
                    segments.add(Segment.of(prevPoint.asPoint2D(), currentPoint.asPoint2D()));
                }
                prevPoint = currentPoint;
            }
        }
        return segments;
    }

    @JsonIgnore
    public int getPointCount() {
        return points.size();
    }

    public Point getPoint(int pointIndex) {
        return points.get(pointIndex);
    }

    public void addPoint(Point point) {
        this.points.add(point);
        segments = null;
    }

    public void addPoint(double x, double y) {
        addPoint(new Point(x, y));
    }

    public void addPoint(Point2D point2D) {
        addPoint(new Point(point2D));
    }

    public boolean updatePoint(int pointIndex, Point newPoint) {
        if (pointIndex >= points.size()) {
            return false;
        } else if (!newPoint.isSame(points.get(pointIndex))) {
            points.set(pointIndex, newPoint);
            segments = null;
            return true;
        } else {
            return false;
        }
    }

    public boolean insertPoint(int pointIndex, Point newPoint) {
        if (pointIndex <= points.size()) {
            points.add(pointIndex, newPoint);
            segments = null;
            return true;
        } else {
            return false;
        }
    }

    public boolean removePoint(int pointIndex) {
        if (pointIndex < points.size()) {
            points.remove(pointIndex);
            segments = null;
            if (points.size() < 3) {
                setClosed(false);
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean closePath() {
        if (points.size() > 2) {
            setClosed(true);
        }
        return isClosed();
    }

    public static Rectangle2D computeBoundingBox(List<Point> points) {
        Point first = points.get(0);
        double minX = first.getX();
        double maxX = minX;
        double minY = first.getY();
        double maxY = minY;
        for (int pointIndex = 1; pointIndex < points.size(); ++pointIndex) {
            Point point = points.get(pointIndex);
            minX = Math.min(minX, point.getX());
            maxX = Math.max(maxX, point.getX());
            minY = Math.min(minY, point.getY());
            maxY = Math.max(maxY, point.getY());
        }
        return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
    }

    public class Handle {
        @Getter
        private final List<Point> originalPoints;

        @Getter
        private final int pointIndex;

        @Getter
        private final Point2D projectedPoint;

        @Getter @Setter
        private boolean moved = false;

        @Getter @Setter
        private Point newPoint;

        public Handle(int pointIndex) {
            this.originalPoints = new ArrayList<>(points);
            this.pointIndex = pointIndex;
            this.projectedPoint = null;
        }

        public Handle(int pointIndex, Point2D projectedPoint) {
            this.originalPoints = new ArrayList<>(points);
            this.pointIndex = pointIndex;
            this.projectedPoint = projectedPoint;
        }

        public Point getOriginalPoint() {
            return originalPoints.get(pointIndex);
        }

        public Point getOriginalPoint(int pointIndex) {
            return originalPoints.get(pointIndex);
        }

        public boolean isProjectedPoint() {
            return projectedPoint != null;
        }

        public Point2D getHandlePoint() {
            return isProjectedPoint() ? projectedPoint : getOriginalPoint().asPoint2D();
        }
    }

    @Override
    public Handle getHandle(Point2D point, Point2D mousePoint, double handleRadius) {
        for (int pointIndex = 0; pointIndex < points.size(); ++pointIndex) {
            Point originalPoint = points.get(pointIndex);
            if (originalPoint.isSame(mousePoint, handleRadius)) {
                log.info("Created handle for point: {}", originalPoint);
                return new Handle(pointIndex);
            }
        }
        List<Segment> segments = getSegments();
        for (int segmentIndex = 0; segmentIndex < segments.size(); ++segmentIndex) {
            Segment segment = segments.get(segmentIndex);
            Point2D projectedPoint = segment.project(mousePoint);
            if (projectedPoint != null) {
                double distanceToMousePoint = projectedPoint.distance(mousePoint);
                if (distanceToMousePoint < handleRadius) {
                    int pointIndex = closed ? ((segmentIndex - 1) % points.size()) : segmentIndex;
                    log.info("Created handle for point: {} on segment: {}", projectedPoint, segment);
                    return new Handle(pointIndex, projectedPoint);
                }
            }
        }
        return null;
    }

    public Handle getHandle(int pointIndex) {
        return new Handle(pointIndex);
    }

    private boolean hasHandleMoved(Handle handle, InteractionEvent event) {
        if (!handle.isMoved() && !event.getPoint().equals(event.getStartPoint())) {
            handle.setMoved(true);
        }
        return handle.isMoved();
    }

    @Override
    public boolean edit(Handle handle, InteractionEvent event) {
        if (hasHandleMoved(handle, event)) {
            if (handle.isProjectedPoint()) {
                Point newPoint = new Point(event.getPoint());
                boolean updated = false;
                if (handle.getNewPoint() == null) {
                    updated = insertPoint(handle.getPointIndex() + 1, newPoint);
                } else {
                    updated = updatePoint(handle.getPointIndex() + 1, newPoint);
                }
                if (updated) {
                    handle.setNewPoint(newPoint);
                }
                return updated;
            } else {
                return updatePoint(handle.getPointIndex(), new Point(event.getPoint()));
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean move(Handle handle, InteractionEvent event) {
        if (hasHandleMoved(handle, event)) {
            Point2D delta = event.getPoint().subtract(handle.getHandlePoint());
            boolean updated = false;
            for (int pointIndex = 0; pointIndex < points.size(); ++pointIndex) {
                if (updatePoint(pointIndex, handle.getOriginalPoint(pointIndex).add(delta))) {
                    updated = true;
                }
            }
            return updated;
        } else {
            return false;
        }
    }

    private static double scaleFactor(double center, double original, double event) {
        if (original > center) {
            event = Math.max(event, center);
            return 1.0 + (event - original) / (original - center);
        } else if (original < center) {
            event = Math.min(event, center);
            return 1.0 + (original - event) / (center - original);
        } else {
            return 0.0;
        }
    }

    private static double scale(double center, double original, double scaleFactor) {
        return center + scaleFactor * (original - center);
    }

    @Override
    public boolean resize(Handle handle, InteractionEvent event) {
        Rectangle2D originalBoundingBox = computeBoundingBox(handle.getOriginalPoints());
        Point2D handlePoint = handle.getHandlePoint();
        double centerX = originalBoundingBox.getMinX() + originalBoundingBox.getWidth() / 2;
        double centerY = originalBoundingBox.getMinY() + originalBoundingBox.getHeight() / 2;
        double scaleFactor = Math.max(
            scaleFactor(centerX, handlePoint.getX(), event.getPoint().getX()),
            scaleFactor(centerY, handlePoint.getY(), event.getPoint().getY()));
        boolean updated = false;
        for (int pointIndex = 0; pointIndex < getPointCount(); ++pointIndex) {
            Point point = handle.getOriginalPoint(pointIndex);
            Point newPoint = new Point(
                    scale(centerX, point.getX(), scaleFactor),
                    scale(centerY, point.getY(), scaleFactor));
            updated = updatePoint(pointIndex, newPoint) || updated;
        }
        return updated;
    }

    @Override
    public Snapshot<Path> save() {
        return new Snapshot<>() {
            private final List<Point> points = new ArrayList<>(getPoints());
            private final boolean closed = isClosed();

            @Override
            public Path restore() {
                Path.this.points.clear();
                Path.this.points.addAll(points);
                Path.this.closed = closed;
                return Path.this;
            }
        };
    }

    @Override
    public void draw(GraphicsContext ctx, double pixelsPerUnit, GridSettings settings) {
        prepareToDraw(ctx, pixelsPerUnit, settings);
        Point prevPoint = closed ? points.get(points.size() - 1) : null;
        for (Point point : points) {
            if (prevPoint != null) {
                ctx.strokeLine(prevPoint.getX(), prevPoint.getY(), point.getX(), point.getY());
            }
            double radius = settings.getShapePointRadius() / pixelsPerUnit;
            ctx.fillOval(point.getX() - radius, point.getY() - radius, radius * 2, radius * 2);
            prevPoint = point;
        }
    }

    @Override
    public String toString() {
        return String.format("Path(%s)", points);
    }
}
