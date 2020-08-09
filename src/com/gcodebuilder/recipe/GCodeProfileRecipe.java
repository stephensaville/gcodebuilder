package com.gcodebuilder.recipe;

import com.gcodebuilder.geometry.Circle;
import com.gcodebuilder.geometry.Rectangle;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.model.ArcDistanceMode;
import com.gcodebuilder.model.Direction;
import com.gcodebuilder.model.DistanceMode;
import com.gcodebuilder.model.FeedRateMode;
import com.gcodebuilder.model.GCodeBuilder;
import com.gcodebuilder.model.LengthUnit;
import com.gcodebuilder.model.LengthUnitConverter;
import com.gcodebuilder.model.MotionMode;
import com.gcodebuilder.model.Side;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Getter
@Setter
public class GCodeProfileRecipe extends GCodeRecipe {
    private static final Logger log = LogManager.getLogger(GCodeProfileRecipe.class);

    private LengthUnit unit = LengthUnit.INCH;
    private double toolWidth = 0.25;
    private double depth = 0.1;
    private double stockSurface = 0;
    private double safetyHeight = 0.5;
    private double stepDown = 0.1;
    private int feedRate = 30;
    private int plungeRate = 30;
    private Side side = Side.OUTSIDE;
    private Direction direction = Direction.CLOCKWISE;

    public GCodeProfileRecipe(int id) {
        super(id, GCodeRecipeType.PROFILE);
        setName(String.format("profile%d", id));
    }

    @Override
    public GCodeProfileRecipe clone() {
        return (GCodeProfileRecipe)super.clone();
    }

    public void convertToUnit(LengthUnit toUnit) {
        LengthUnitConverter converter = unit.getConverterTo(toUnit);
        setUnit(toUnit);
        setToolWidth(converter.convert(toolWidth));
        setDepth(converter.convert(depth));
        setStockSurface(converter.convert(stockSurface));
        setSafetyHeight(converter.convert(safetyHeight));
        setStepDown(converter.convert(stepDown));
        setFeedRate((int)Math.round(converter.convert(feedRate)));
        setPlungeRate((int)Math.round(converter.convert(plungeRate)));
    }

    @Override
    public void generateGCode(Shape<?> shape, GCodeBuilder builder) {
        if (unit.getMode() != builder.getUnitMode()) {
            LengthUnit builderUnit = LengthUnit.fromUnitMode(builder.getUnitMode());
            GCodeProfileRecipe convertedRecipe = clone();
            convertedRecipe.convertToUnit(builderUnit);
            convertedRecipe.generateGCode(shape, builder);
        }
        if (shape instanceof Rectangle) {
            generateRectangleGCode((Rectangle)shape, builder);
        } else if (shape instanceof Circle) {
            generateCircleGCode((Circle)shape, builder);
        } else {
            throw new UnsupportedOperationException(String.format(
                    "Cannot generate %s type GCode for %s shape.",
                    getType(), shape.getClass().getSimpleName()));
        }
    }

    public void generateRectangleGCode(Rectangle shape, GCodeBuilder builder) {
        log.info("Generating GCode for:{}", shape);

        builder .distanceMode(DistanceMode.ABSOLUTE)
                .feedRateMode(FeedRateMode.UNITS_PER_MIN);

        double toolOffset = toolWidth/2 * side.getOffsetSign();

        double minX = shape.getMinX() - toolOffset;
        double minY = shape.getMinY() - toolOffset;
        double maxX = shape.getMaxX() + toolOffset;
        double maxY = shape.getMaxY() + toolOffset;

        double currentZ = stockSurface;
        double minZ = stockSurface - depth;

        while (currentZ > minZ) {
            // step down or bottom out
            double cutToZ = Math.max(minZ, currentZ - stepDown);

            // move over starting point
            builder .motionMode(MotionMode.RAPID_LINEAR)
                    .Z(safetyHeight).endLine()
                    .XY(minX, minY).endLine();

            // plunge down to cut depth
            builder .motionMode(MotionMode.LINEAR).feedRate(plungeRate)
                    .Z(cutToZ).endLine()
                    .feedRate(feedRate);

            // cut profile in XY plane
            switch (direction) {
                case CLOCKWISE:
                    builder .XY(minX, maxY).endLine()
                            .XY(maxX, maxY).endLine()
                            .XY(maxX, minY).endLine()
                            .XY(minX, minY).endLine();
                    break;
                case COUNTER_CLOCKWISE:
                    builder .XY(maxX, minY).endLine()
                            .XY(maxX, maxY).endLine()
                            .XY(minX, maxY).endLine()
                            .XY(minX, minY).endLine();
                    break;
            }

            // update current depth
            currentZ = cutToZ;
        }

        builder .motionMode(MotionMode.RAPID_LINEAR)
                .Z(safetyHeight).endLine();
    }

    public void generateCircleGCode(Circle shape, GCodeBuilder builder) {
        log.info("Generating GCode for:{}", shape);

        builder .distanceMode(DistanceMode.ABSOLUTE)
                .arcDistanceMode(ArcDistanceMode.INCREMENTAL)
                .feedRateMode(FeedRateMode.UNITS_PER_MIN);

        double toolOffset = toolWidth/2 * side.getOffsetSign();

        double startX = shape.getMinX() - toolOffset;
        double startY = shape.getCenter().getY();
        double offsetCenterX = shape.getCenter().getX() - startX;
        double offsetCenterY = 0;

        double currentZ = stockSurface;
        double minZ = stockSurface - depth;

        while (currentZ > minZ) {
            // step down or bottom out
            double cutToZ = Math.max(minZ, currentZ - stepDown);

            // move over starting point
            builder .motionMode(MotionMode.RAPID_LINEAR)
                    .Z(safetyHeight).endLine()
                    .XY(startX, startY).endLine();

            // plunge down to cut depth
            builder .motionMode(MotionMode.LINEAR).feedRate(plungeRate)
                    .Z(cutToZ).endLine();

            // cut circular profile
            switch (direction) {
                case CLOCKWISE:
                    builder.motionMode(MotionMode.CW_ARC).feedRate(feedRate)
                            .XY(startX, startY)
                            .IJ(offsetCenterX, offsetCenterY)
                            .endLine();
                    break;
                case COUNTER_CLOCKWISE:
                    builder.motionMode(MotionMode.CCW_ARC).feedRate(feedRate)
                            .XY(startX, startY)
                            .IJ(offsetCenterX, offsetCenterY)
                            .endLine();
                    break;
            }

            // update current depth
            currentZ = cutToZ;
        }

        builder .motionMode(MotionMode.RAPID_LINEAR)
                .Z(safetyHeight).endLine();
    }
}
