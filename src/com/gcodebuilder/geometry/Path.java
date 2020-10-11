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
import javafx.scene.shape.StrokeLineCap;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonTypeName("PATH")
public class Path extends SimpleShape<Path.Handle> {
    private static final Logger log = LogManager.getLogger(Path.class);

    private List<Point> points;
    private volatile List<PathSegment> segments;

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
    public List<PathSegment> getSegments() {
        if (points.size() < 2) {
            return Collections.emptyList();
        }
        if (segments == null) {
            segments = new ArrayList<>();
            for (int i = 0; i < points.size(); i++) {
                Point from = getPoint(i);
                Point to = getNextPoint(i);
                if (to != null && to.isCenterPoint()) {
                    Point center = to;
                    to = getNextPoint(++i);
                    if (to != null) {
                        segments.add(ArcSegment.of(from, center, to));
                    }
                } else if (to != null) {
                    segments.add(LineSegment.of(from, to));
                }
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

    public int getNextPointIndex(int pointIndex) {
        int nextPointIndex = pointIndex + 1;
        if (0 <= nextPointIndex && nextPointIndex < points.size()) {
            return nextPointIndex;
        } else if (nextPointIndex == points.size() && closed) {
            return 0;
        } else {
            return -1;
        }
    }

    public Point getNextPoint(int pointIndex) {
        int nextPointIndex = getNextPointIndex(pointIndex);
        return (nextPointIndex != -1) ? points.get(nextPointIndex) : null;
    }

    public int getPrevPointIndex(int pointIndex) {
        int prevPointIndex = pointIndex - 1;
        if (0 <= prevPointIndex && prevPointIndex < points.size()) {
            return prevPointIndex;
        } else if (prevPointIndex == -1 && closed) {
            return points.size() - 1;
        } else {
            return -1;
        }
    }

    public Point getPrevPoint(int pointIndex) {
        int prevPointIndex = getPrevPointIndex(pointIndex);
        return (prevPointIndex != -1) ? points.get(prevPointIndex) : null;
    }

    private boolean repairArcSegment(int pointIndex, int centerPointIndex) {
        int fromPointIndex = getPrevPointIndex(centerPointIndex);
        int toPointIndex = getNextPointIndex(centerPointIndex);
        log.info("Repairing arc: fromPointIndex={} centerPointIndex={} toPointIndex={}",
                fromPointIndex, centerPointIndex, toPointIndex);
        if (fromPointIndex >= 0 && toPointIndex >= 0) {
            Point from = points.get(fromPointIndex);
            Point center = points.get(centerPointIndex);
            Point to = points.get(toPointIndex);
            if (pointIndex == toPointIndex) {
                ArcSegment arc = ArcSegment.of(to, center, from, !center.isClockwiseCenterPoint());
                if (!from.isSame(arc.getTo())) {
                    points.set(fromPointIndex, new Point(arc.getTo()));
                    segments = null;
                    return true;
                }
            } else {
                ArcSegment arc = ArcSegment.of(from, center, to);
                if (!to.isSame(arc.getTo())) {
                    points.set(toPointIndex, new Point(arc.getTo()));
                    segments = null;
                    return true;
                }
            }
        }
        return false;
    }

    private void repairArcSegment(int pointIndex) {
        int updatedFromPointIndex = -1;
        int updatedToPointIndex = -1;
        if (points.get(pointIndex).isCenterPoint()) {
            // updated point is arc center
            log.info("Updated arc center at index: {}", pointIndex);
            if (repairArcSegment(pointIndex, pointIndex)) {
                updatedToPointIndex = getNextPointIndex(pointIndex);
            }
        }
        if (pointIndex > 0 && points.get(pointIndex-1).isCenterPoint()) {
            // updated point is arc to point
            log.info("Updated arc to point at index: {}", pointIndex);
            if (repairArcSegment(pointIndex, pointIndex - 1)) {
                updatedFromPointIndex = getPrevPointIndex(pointIndex - 1);
            }
        }
        if (pointIndex + 1 < points.size() && points.get(pointIndex+1).isCenterPoint()) {
            // updated point is arc from point
            log.info("Updated arc from point at index: {}", pointIndex);
            if (repairArcSegment(pointIndex, pointIndex + 1)) {
                updatedToPointIndex = getNextPointIndex(pointIndex + 1);
            }
        }
        while (updatedFromPointIndex != -1 || updatedToPointIndex != -1) {
            if (updatedFromPointIndex != -1) {
                int updatedFromPointPrevIndex = getPrevPointIndex(updatedFromPointIndex);
                if (updatedFromPointPrevIndex != pointIndex && updatedFromPointPrevIndex != -1
                        && points.get(updatedFromPointPrevIndex).isCenterPoint()) {
                    int updatedFromPointPrevPrevIndex = getPrevPointIndex(updatedFromPointIndex);
                    if (updatedFromPointPrevPrevIndex != pointIndex
                            && repairArcSegment(updatedFromPointIndex, updatedFromPointPrevIndex)) {
                        updatedFromPointIndex = updatedFromPointPrevPrevIndex;
                    } else {
                        updatedFromPointIndex = -1;
                    }
                } else {
                    updatedFromPointIndex = -1;
                }
            }
            if (updatedToPointIndex != -1) {
                int updatedToPointNextIndex = getNextPointIndex(updatedToPointIndex);
                if (updatedToPointNextIndex != pointIndex && updatedToPointNextIndex != -1
                        && points.get(updatedToPointNextIndex).isCenterPoint()) {
                    int updatedToPointNextNextIndex = getNextPointIndex(updatedToPointNextIndex);
                    if (updatedToPointNextNextIndex != pointIndex
                            && repairArcSegment(updatedToPointIndex, updatedToPointNextIndex)) {
                        updatedToPointIndex = updatedToPointNextNextIndex;
                    } else {
                        updatedToPointIndex = -1;
                    }
                } else {
                    updatedToPointIndex = -1;
                }
            }
        }
    }

    public void addPoint(Point point) {
        this.points.add(point);
        repairArcSegment(points.size() - 1);
        segments = null;
    }

    public void addPoint(double x, double y, Point.Type type) {
        addPoint(new Point(x, y, type));
    }

    public void addPoint(double x, double y) {
        addPoint(x, y, null);
    }

    public void addPoint(Point2D point2D, Point.Type type) {
        addPoint(new Point(point2D, type));
    }

    public void addPoint(Point2D point2D) {
        addPoint(point2D, null);
    }

    public boolean updatePoint(int pointIndex, Point newPoint) {
        if (pointIndex >= points.size()) {
            return false;
        } else if (!newPoint.isSame(points.get(pointIndex))) {
            points.set(pointIndex, newPoint);
            repairArcSegment(pointIndex);
            segments = null;
            return true;
        } else {
            return false;
        }
    }

    public boolean insertPoint(int pointIndex, Point newPoint) {
        if (pointIndex <= points.size()) {
            points.add(pointIndex, newPoint);
            repairArcSegment(pointIndex);
            segments = null;
            return true;
        } else {
            return false;
        }
    }

    public boolean removePoint(int pointIndex) {
        if (pointIndex < points.size()) {
            points.remove(pointIndex);
            if (pointIndex < points.size()) {
                repairArcSegment(pointIndex);
            }
            segments = null;
            if (points.size() < 2) {
                setClosed(false);
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean closePath() {
        if (points.size() > 1) {
            setClosed(true);
            segments = null;
        }
        return isClosed();
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
        List<PathSegment> segments = getSegments();
        for (int segmentIndex = 0, pointIndex = 0; segmentIndex < segments.size(); ++segmentIndex, ++pointIndex) {
            PathSegment segment = segments.get(segmentIndex);
            Point2D projectedPoint = segment.project(mousePoint);
            if (projectedPoint != null) {
                double distanceToMousePoint = projectedPoint.distance(mousePoint);
                if (distanceToMousePoint < handleRadius) {
                    log.info("Created handle for point: {} on segment: {}", projectedPoint, segment);
                    return new Handle(pointIndex, projectedPoint);
                }
            }
            if (segment instanceof ArcSegment) {
                ++pointIndex;
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
                boolean updated;
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
                return updatePoint(handle.getPointIndex(),
                        new Point(event.getPoint(), handle.getOriginalPoint().getType()));
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean move(Point2D delta) {
        if (delta.getX() == 0 && delta.getY() == 0) {
            return false;
        }
        for (int pointIndex = 0; pointIndex < points.size(); ++pointIndex) {
            points.set(pointIndex, getPoint(pointIndex).add(delta));
        }
        segments = null;
        return true;
    }

    private static double scale(double center, double original, double scaleFactor) {
        return center + scaleFactor * (original - center);
    }

    @Override
    @JsonIgnore
    public Rectangle2D getBoundingBox() {
        return Math2D.computeBoundingBoxForPoints(getPoints());
    }

    @Override
    @JsonIgnore
    public Point getCenter() {
        return new Point(Math2D.getBoundingBoxCenter(getBoundingBox()));
    }

    @Override
    public boolean resize(double scaleFactor, Point center) {
        if (scaleFactor == 1.0) {
            return false;
        }
        for (int pointIndex = 0; pointIndex < getPointCount(); ++pointIndex) {
            Point point = getPoint(pointIndex);
            Point newPoint = new Point(
                    scale(center.getX(), point.getX(), scaleFactor),
                    scale(center.getY(), point.getY(), scaleFactor),
                    point.getType());
            points.set(pointIndex, newPoint);
        }
        segments = null;
        return true;
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
                Path.this.segments = null;
                return Path.this;
            }
        };
    }

    @Override
    public Path convertToPath() {
        return this;
    }

    @Override
    public void draw(GraphicsContext ctx, double pixelsPerUnit, GridSettings settings) {
        prepareToDraw(ctx, pixelsPerUnit, settings);
        ctx.setLineCap(StrokeLineCap.ROUND);
        for (PathSegment segment : getSegments()) {
            segment.draw(ctx);
        }
        if (isSelected()) {
            double pointRadius = settings.getShapePointRadius() / pixelsPerUnit;
            for (Point point : getPoints()) {
                ctx.fillOval(point.getX() - pointRadius, point.getY() - pointRadius,
                        pointRadius * 2, pointRadius * 2);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Path[");
        boolean first = true;
        for (Point p : points) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(p.toCoordinateString());
        }
        sb.append("]");
        return sb.toString();
    }
}
