package com.gcodebuilder.canvas;

import com.gcodebuilder.app.GridSettings;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.adapter.ReadOnlyJavaBeanObjectProperty;
import javafx.beans.property.adapter.ReadOnlyJavaBeanObjectPropertyBuilder;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.stage.Screen;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class GCodeCanvas extends Canvas {
    private static final Logger log = LogManager.getLogger(GCodeCanvas.class);

    @Getter @Setter
    private GridSettings settings = new GridSettings();

    @Getter @Setter
    private double zoom = 1;

    @Getter @Setter
    private double originX = 0;

    @Getter @Setter
    private double originY = 0;

    private double canvasWidth = 0;
    private double canvasHeight = 0;

    @Getter
    private Rectangle2D originArea;
    private final ReadOnlyJavaBeanObjectProperty<Rectangle2D> originAreaProperty;

    private static final Affine IDENTITY_TRANSFORM = new Affine();

    @Getter
    private final Affine gridToScreen = new Affine();

    @Getter
    private final Affine screenToGrid = new Affine();

    @Getter
    private final List<Drawable> drawables = new ArrayList<>();

    public GCodeCanvas() {
        this(0,0);
    }

    public GCodeCanvas(double width, double height) {
        super(width, height);
        try {
            originAreaProperty = ReadOnlyJavaBeanObjectPropertyBuilder
                    .<Rectangle2D>create().bean(this).name("originArea").build();
            updateOriginArea(getPixelsPerUnit(), width, height);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public void resize(double width, double height) {
        super.resize(width, height);
        log.debug("resize({}, {})", width, height);
        if (width != canvasWidth || height != canvasHeight) {
            refresh();
        }
    }

    public double getPixelsPerUnit() {
        return zoom * Screen.getPrimary().getDpi() / settings.getUnits().getUnitsPerInch();
    }

    public double calculateZoom(double pixelsPerUnit) {
        return pixelsPerUnit * settings.getUnits().getUnitsPerInch() / Screen.getPrimary().getDpi();
    }

    private boolean applyGridLineSettings(GraphicsContext ctx, int gridIndex, double pixelsPerUnit,
                                          double pixelsPerGridLine) {
        if (gridIndex % settings.getMinorGridDivision() == 0) {
            // major grid line
            ctx.setLineWidth(settings.getMajorGridLineWidth() / pixelsPerUnit);
            ctx.setStroke(settings.getMajorGridPaint());
            return pixelsPerGridLine * settings.getMinorGridDivision() > settings.getMinPixelsPerGridLine();
        } else {
            // minor grid line
            ctx.setLineWidth(settings.getMinorGridLineWidth() / pixelsPerUnit);
            ctx.setStroke(settings.getMinorGridPaint());
            return pixelsPerGridLine > settings.getMinPixelsPerGridLine();
        }
    }

    private void updateOriginArea(double pixelsPerUnit, double width, double height) {
        Rectangle2D drawingArea = settings.getDrawingArea();
        double minOriginX = width - pixelsPerUnit * drawingArea.getMaxX();
        double maxOriginX = - pixelsPerUnit * drawingArea.getMinX();
        if (minOriginX > maxOriginX) {
            minOriginX = maxOriginX = width / 2;
        }
        double minOriginY = height + pixelsPerUnit * drawingArea.getMinY();
        double maxOriginY = pixelsPerUnit * drawingArea.getMaxY();
        if (minOriginY > maxOriginY) {
            minOriginY = maxOriginY = height / 2;
        }
        originArea = new Rectangle2D(minOriginX, minOriginY, maxOriginX - minOriginX, maxOriginY - minOriginY);
        originX = Math.max(minOriginX, Math.min(maxOriginX, originX));
        originY = Math.max(minOriginY, Math.min(maxOriginY, originY));
        originAreaProperty.fireValueChangedEvent();
    }

    public ReadOnlyProperty<Rectangle2D> originAreaProperty() {
        return originAreaProperty;
    }

    public void refresh() {
        double pixelsPerUnit = getPixelsPerUnit();
        canvasWidth = getWidth();
        canvasHeight = getHeight();
        updateOriginArea(getPixelsPerUnit(), canvasWidth, canvasHeight);

        double gridSpacing = settings.getMinorGridSpacing();
        double pixelsPerGridLine = pixelsPerUnit * gridSpacing;
        int xGridMinIndex = -(int)(originX / pixelsPerGridLine);
        int xGridMaxIndex = (int)((canvasWidth - originX) / pixelsPerGridLine);
        int yGridMinIndex = -(int)((canvasHeight - originY) / pixelsPerGridLine);
        int yGridMaxIndex = (int)(originY / pixelsPerGridLine);

        // clear screen
        GraphicsContext ctx = getGraphicsContext2D();
        ctx.setTransform(IDENTITY_TRANSFORM);
        ctx.clearRect(0, 0, canvasWidth, canvasHeight);

        // set grid transform
        gridToScreen.setToTransform(pixelsPerUnit, 0, originX, 0, -pixelsPerUnit, originY);
        screenToGrid.setToTransform(1/pixelsPerUnit, 0, -originX/pixelsPerUnit, 0, -1/pixelsPerUnit, originY/pixelsPerUnit);
        ctx.setTransform(gridToScreen);
        Bounds visibleBounds = getBoundsInLocal();
        try {
            visibleBounds = ctx.getTransform().inverseTransform(visibleBounds);
        } catch (NonInvertibleTransformException ex) {

        }

        // draw grid
        for (int xGridIndex = xGridMinIndex; xGridIndex <= xGridMaxIndex; xGridIndex++) {
            if (applyGridLineSettings(ctx, xGridIndex, pixelsPerUnit, pixelsPerGridLine)) {
                double xGridPos = xGridIndex * gridSpacing;
                ctx.strokeLine(xGridPos, visibleBounds.getMinY(), xGridPos, visibleBounds.getMaxY());
            }
        }
        for (int yGridIndex = yGridMinIndex; yGridIndex <= yGridMaxIndex; yGridIndex++) {
            if (applyGridLineSettings(ctx, yGridIndex, pixelsPerUnit, pixelsPerGridLine)) {
                double yGridPos = yGridIndex * gridSpacing;
                ctx.strokeLine(visibleBounds.getMinX(), yGridPos, visibleBounds.getMaxX(), yGridPos);
            }
        }

        // draw axes
        ctx.setLineWidth(settings.getAxisLineWidth() / pixelsPerUnit);
        ctx.setStroke(settings.getXAxisPaint());
        ctx.strokeLine(visibleBounds.getMinX(), 0, visibleBounds.getMaxX(), 0);
        ctx.setStroke(settings.getYAxisPaint());
        ctx.strokeLine(0, visibleBounds.getMinY(), 0, visibleBounds.getMaxY());

        for (Drawable drawable : drawables) {
            if (drawable != null && drawable.isVisible()) {
                drawable.draw(ctx, pixelsPerUnit, settings);
            }
        }
    }

    public Point2D snapToGrid(double x, double y) {
        double gridSpacing = settings.getMajorGridSpacing() / settings.getMinorGridDivision();
        return new Point2D(Math.rint(x / gridSpacing)*gridSpacing,
                Math.rint(y / gridSpacing)*gridSpacing);
    }

    public Point2D mouseToGrid(MouseEvent event, boolean snapToGrid) {
        Point2D gridPoint = screenToGrid.transform(event.getX(), event.getY());
        if (snapToGrid) {
            return snapToGrid(gridPoint.getX(), gridPoint.getY());
        } else {
            return gridPoint;
        }
    }
}
