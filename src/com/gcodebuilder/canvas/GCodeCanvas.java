package com.gcodebuilder.canvas;

import com.gcodebuilder.model.UnitMode;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.transform.Affine;
import javafx.stage.Screen;
import lombok.Getter;

public class GCodeCanvas extends Canvas {
    private static final Paint GRID_PAINT = Color.color(0.4, 0.4, 0.4, 0.6);
    private static final Paint X_AXIS_PAINT = Color.GREEN;
    private static final Paint Y_AXIS_PAINT = Color.RED;

    @Getter
    private UnitMode unitMode = UnitMode.INCH;

    @Getter
    private double zoom = 1;

    @Getter
    private double originX = 0;

    @Getter
    private double originY = 0;

    @Getter
    private double gridSpacing = 1;

    private double canvasWidth = 0;
    private double canvasHeight = 0;

    public GCodeCanvas() {
    }

    public GCodeCanvas(double width, double height) {
        super(width, height);
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public void resize(double width, double height) {
        super.resize(width, height);
        System.out.println(String.format("resize(%f,%f)", width, height));
        if (width != canvasWidth || height != canvasHeight) {
            updateGrid();
        }
    }

    public void setUnitMode(UnitMode unitMode) {
        this.unitMode = unitMode;
        updateGrid();
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
        updateGrid();
    }

    public void setOriginX(double originX) {
        this.originX = originX;
        updateGrid();
    }

    public void setOriginY(double originY) {
        this.originY = originY;
        updateGrid();
    }

    public void setGridSpacing(double gridSpacing) {
        this.gridSpacing = gridSpacing;
        updateGrid();
    }

    public double getPixelsPerUnit() {
        return zoom * Screen.getPrimary().getDpi() / unitMode.getUnitsPerInch();
    }

    private void updateGrid() {
        double pixelsPerUnit = getPixelsPerUnit();
        canvasWidth = getWidth();
        canvasHeight = getHeight();
        double pixelsPerGridLine = pixelsPerUnit * gridSpacing;
        int hGridStart = -(int)(originX / pixelsPerGridLine);
        int hGridStop = (int)((canvasWidth - originX) / pixelsPerGridLine);
        int vGridStart = -(int)((canvasHeight - originY) / pixelsPerGridLine);
        int vGridStop = (int)(originY / pixelsPerGridLine);

        System.out.println(String.format("START updateGrid: pixelsPerUnit=%f canvasWidth=%f canvasHeight=%f pixelsPerGridLine=%f hGrid=[%d,%d] vGrid=[%d,%d]",
                pixelsPerUnit, canvasWidth, canvasHeight, pixelsPerGridLine, hGridStart, hGridStop, vGridStart, vGridStop));

        // clear screen
        GraphicsContext ctx = getGraphicsContext2D();
        ctx.setTransform(new Affine());
        ctx.clearRect(0, 0, canvasWidth, canvasHeight);

        // set grid transform
        ctx.setTransform(pixelsPerUnit, 0, 0, -pixelsPerUnit, originX, originY);

        // draw grid
        ctx.setStroke(GRID_PAINT);
        ctx.setLineWidth(0.5 / pixelsPerUnit);
        for (int hGridIndex = hGridStart; hGridIndex <= hGridStop; hGridIndex++) {
            ctx.strokeLine(hGridIndex*gridSpacing, (vGridStart- 1)*gridSpacing, hGridIndex*gridSpacing, (vGridStop+1)*gridSpacing);
        }
        for (int vGridIndex = vGridStart; vGridIndex <= vGridStop; vGridIndex++) {
            ctx.strokeLine((hGridStart-1)*gridSpacing, vGridIndex*gridSpacing, (hGridStop+1)*gridSpacing, vGridIndex*gridSpacing);
        }

        // draw axes
        ctx.setLineWidth(3 / pixelsPerUnit);
        ctx.setStroke(X_AXIS_PAINT);
        ctx.strokeLine(0, 0, gridSpacing * 2, 0);
        ctx.setStroke(Y_AXIS_PAINT);
        ctx.strokeLine(0, 0, 0, gridSpacing * 2);

        System.out.println("END updateGrid");
    }
}
