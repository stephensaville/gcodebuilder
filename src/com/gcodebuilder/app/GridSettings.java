package com.gcodebuilder.app;

import com.gcodebuilder.model.LengthUnit;
import com.gcodebuilder.model.LengthUnitConverter;
import com.gcodebuilder.model.UnitMode;
import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.Map;

@Data
public class GridSettings implements Cloneable {
    private static final Logger log = LogManager.getLogger(GridSettings.class);

    private LengthUnit units = LengthUnit.INCH;

    private Rectangle2D drawingArea = new Rectangle2D(-16, -16, 32, 32);

    private static Map<LengthUnit, Double> MAJOR_GRID_SPACING_DEFAULTS = new EnumMap<>(LengthUnit.class) {
        {
            put(LengthUnit.INCH, 1.0);
            put(LengthUnit.MM, 10.0);
        }
    };
    private double majorGridSpacing = MAJOR_GRID_SPACING_DEFAULTS.get(units);
    private Paint majorGridPaint = Color.color(0, 0, 0.5, 0.6);
    private double majorGridLineWidth = 0.5;

    private static Map<LengthUnit, Integer> MINOR_GRID_DIVISION_DEFAULTS = new EnumMap<>(LengthUnit.class) {
        {
            put(LengthUnit.INCH, 8);
            put(LengthUnit.MM, 10);
        }
    };
    private int minorGridDivision = MINOR_GRID_DIVISION_DEFAULTS.get(units);
    private Paint minorGridPaint = Color.color(0, 0, 0.5, 0.4);
    private double minorGridLineWidth = 0.25;

    private int minPixelsPerGridLine = 4;

    private Paint xAxisPaint = Color.GREEN;
    private Paint yAxisPaint = Color.RED;
    private double axisLineWidth = 3;

    private double shapeLineWidth = 2;
    private double shapePointRadius = 4;
    private Paint shapePaint = Color.BLACK;
    private Paint selectedShapePaint = Color.BLUE;

    public void setUnits(LengthUnit units) {
        if (units != this.units) {
            log.info("Changing units from {} to {}", this.units, units);
            LengthUnitConverter converter = this.units.getConverterTo(units);
            this.drawingArea = new Rectangle2D(
                    converter.convert(this.drawingArea.getMinX()),
                    converter.convert(this.drawingArea.getMinY()),
                    converter.convert(this.drawingArea.getWidth()),
                    converter.convert(this.drawingArea.getHeight()));
            if (MAJOR_GRID_SPACING_DEFAULTS.get(this.units).doubleValue() == majorGridSpacing) {
                majorGridSpacing = MAJOR_GRID_SPACING_DEFAULTS.get(units);
            }
            if (MINOR_GRID_DIVISION_DEFAULTS.get(this.units).intValue() == minorGridDivision) {
                minorGridDivision = MINOR_GRID_DIVISION_DEFAULTS.get(units);
            }
            this.units = units;
        }
    }

    public void setDefaultGridSpacing() {
        majorGridSpacing = MAJOR_GRID_SPACING_DEFAULTS.get(units);
        minorGridDivision = MINOR_GRID_DIVISION_DEFAULTS.get(units);
    }

    public double getMinorGridSpacing() {
        return majorGridSpacing / minorGridDivision;
    }

    @Override
    protected GridSettings clone() {
        try {
            return (GridSettings) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
