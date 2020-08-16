package com.gcodebuilder.geometry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gcodebuilder.app.GridSettings;
import com.gcodebuilder.app.tools.InteractionEvent;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Path extends Shape<Path.Handle> {
    private static final Logger log = LogManager.getLogger(Path.class);

    private List<Point> points;
    private List<Segment> segments;

    @Getter @Setter
    private boolean closed;

    @JsonCreator
    public Path(@JsonProperty("points") List<Point> points,
                @JsonProperty("closed") boolean closed) {
        this.points = new ArrayList<>(points);
        this.closed = closed;
    }

    public Path() {
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

    public void addPoint(Point2D point2D) {
        addPoint(new Point(point2D));
    }

    public boolean updatePoint(int pointIndex, Point newPoint) {
        if (pointIndex >= points.size()) {
            return false;
        } else if (!newPoint.equals(points.get(pointIndex))) {
            points.set(pointIndex, newPoint);
            segments = null;
            return true;
        } else {
            return false;
        }
    }

    public class Handle {
        private final List<Point> originalPoints;

        @Getter
        private final int pointIndex;

        @Getter @Setter
        private boolean moved = false;

        public Handle(int pointIndex) {
            this.originalPoints = new ArrayList<>(points);
            this.pointIndex = pointIndex;
        }

        public Point getOriginalPoint() {
            return originalPoints.get(pointIndex);
        }

        public Point getOriginalPoint(int pointIndex) {
            return originalPoints.get(pointIndex);
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
            return updatePoint(handle.getPointIndex(), new Point(event.getPoint()));
        } else {
            return false;
        }
    }

    @Override
    public boolean move(Handle handle, InteractionEvent event) {
        if (hasHandleMoved(handle, event)) {
            Point2D delta = event.getPoint().subtract(handle.getOriginalPoint().asPoint2D());
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
