package com.gcodebuilder.app;

import com.gcodebuilder.model.UnitMode;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import lombok.Data;

@Data
public class GridSettings {
    private UnitMode units = UnitMode.INCH;

    private Rectangle2D drawingArea = new Rectangle2D(-16, -16, 32, 32);

    private double majorGridSpacing = 1.0;
    private Paint majorGridPaint = Color.color(0, 0, 0.5, 0.6);
    private double majorGridLineWidth = 0.5;

    private int minorGridDivision = 8;
    private Paint minorGridPaint = Color.color(0, 0, 0.5, 0.4);
    private double minorGridLineWidth = 0.25;

    private int minPixelsPerGridLine = 4;

    private Paint xAxisPaint = Color.GREEN;
    private Paint yAxisPaint = Color.RED;
    private double axisLineWidth = 3;
}