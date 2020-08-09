package com.gcodebuilder.recipe;

import com.gcodebuilder.geometry.Circle;
import com.gcodebuilder.geometry.Rectangle;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.model.Direction;
import com.gcodebuilder.model.DistanceMode;
import com.gcodebuilder.model.FeedRateMode;
import com.gcodebuilder.model.GCodeBuilder;
import com.gcodebuilder.model.LengthUnit;
import com.gcodebuilder.model.LengthUnitConverter;
import com.gcodebuilder.model.MotionMode;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Getter
@Setter
public class GCodePocketRecipe extends GCodeRecipe {
    private static final Logger log = LogManager.getLogger(GCodePocketRecipe.class);

    private LengthUnit unit = LengthUnit.INCH;
    private double toolWidth = 0.25;
    private double depth = 0.1;
    private double stockSurface = 0;
    private double safetyHeight = 0.5;
    private double stepDown = 0.1;
    private double stepOver = 40;
    private int feedRate = 30;
    private int plungeRate = 30;
    private Direction direction = Direction.CLOCKWISE;

    public GCodePocketRecipe(int id) {
        super(id, GCodeRecipeType.POCKET);
        setName(String.format("pocket%d", id));
    }

    @Override
    public GCodePocketRecipe clone() {
        return (GCodePocketRecipe)super.clone();
    }

    public void convertToUnit(LengthUnit toUnit) {
        //UnitConverter converter = unit.getUnit().getConverterTo(toUnit.getUnit());
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
            GCodePocketRecipe convertedRecipe = clone();
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
        log.info("Generating pocket gcode for:{}", shape);

        double shortDimension = Math.min(shape.getWidth(), shape.getHeight());
        if (toolWidth > shortDimension) {
            log.warn("Shape:{} is too small to cut pocket using toolWidth:{}", shape, toolWidth);
            return;
        }

        double toolOffset = toolWidth / 2;
        double stepWidth = toolWidth * stepOver / 100.0;

        // calculate bounds of final (inside profile) cut
        double minX = shape.getMinX() + toolOffset;
        double minY = shape.getMinY() + toolOffset;
        double maxX = shape.getMaxX() - toolOffset;
        double maxY = shape.getMaxY() - toolOffset;
        log.info("minX={} minY={} maxX={} maxY={}", minX, minY, maxX, maxY);

        // calculate bounds of starting (center) cut
        double startOffset = Math.max(toolOffset, (shortDimension - stepWidth) / 2);
        double startMinX = shape.getMinX() + startOffset;
        double startMinY = shape.getMinY() + startOffset;
        double startMaxX = shape.getMaxX() - startOffset;
        double startMaxY = shape.getMaxY() - startOffset;
        log.info("stepWidth={} startOffset={} startMinX={} startMinY={} startMaxX={} startMaxY={}",
                stepWidth, startOffset, startMinX, startMinY, startMaxX, startMaxY);

        double currentZ = stockSurface;
        double minZ = stockSurface - depth;

        builder .distanceMode(DistanceMode.ABSOLUTE)
                .feedRateMode(FeedRateMode.UNITS_PER_MIN);

        while (currentZ > minZ) {
            // step down or bottom out
            double cutToZ = Math.max(minZ, currentZ - stepDown);

            // move over starting point
            builder .motionMode(MotionMode.RAPID_LINEAR)
                    .Z(safetyHeight).endLine()
                    .XY(startMinX, startMinY).endLine();

            // plunge down to cut depth
            builder .motionMode(MotionMode.LINEAR).feedRate(plungeRate)
                    .Z(cutToZ).endLine()
                    .feedRate(feedRate);

            double currentMinY = startMinY;
            double currentMinX = startMinX;
            double currentMaxY = startMaxY;
            double currentMaxX = startMaxX;

            // cut pocket in XY plane
            while (true) {
                // cut around current rectangle
                switch (direction) {
                    case CLOCKWISE:
                        builder .XY(currentMinX, currentMaxY).endLine()
                                .XY(currentMaxX, currentMaxY).endLine()
                                .XY(currentMaxX, currentMinY).endLine()
                                .XY(currentMinX, currentMinY).endLine();
                        break;
                    case COUNTER_CLOCKWISE:
                        builder .XY(currentMaxX, currentMinY).endLine()
                                .XY(currentMaxX, currentMaxY).endLine()
                                .XY(currentMinX, currentMaxY).endLine()
                                .XY(currentMinX, currentMinY).endLine();
                        break;
                }

                if (currentMinX > minX || currentMinY > minY) {
                    currentMinX = Math.max(minX, currentMinX - stepWidth);
                    currentMinY = Math.max(minY, currentMinY - stepWidth);
                    currentMaxX = Math.min(maxX, currentMaxX + stepWidth);
                    currentMaxY = Math.min(maxY, currentMaxY + stepWidth);

                    // cut to start of next rectangle
                    builder.XY(currentMinX, currentMinY).endLine();
                } else {
                    break;
                }
            }

            // update current depth
            currentZ = cutToZ;
        }

        builder .motionMode(MotionMode.RAPID_LINEAR)
                .Z(safetyHeight).endLine();
    }

    public void generateCircleGCode(Circle shape, GCodeBuilder builder) {
        log.info("Generating pocket gcode for:{}", shape);

        double toolOffset = toolWidth / 2;
        double stepWidth = toolWidth * stepOver / 100.0;

        if (toolOffset > shape.getRadius()) {
            log.warn("Shape:{} is too small to cut pocket using toolWidth:{}", shape, toolWidth);
            return;
        }

        // calculate bounds of final (inside profile) cut
        double minX = shape.getMinX() + toolOffset;

        // calculate bounds of starting (center) cut
        double startOffset = Math.max(toolOffset, shape.getRadius() - stepWidth / 2);
        double startX = shape.getMinX() + startOffset;
        double startY = shape.getCenter().getY();

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

            double currentX = startX;

            while (true) {
                double offsetCenterX = shape.getCenter().getX() - currentX;
                double offsetCenterY = 0;

                // cut circular profile
                switch (direction) {
                    case CLOCKWISE:
                        builder .motionMode(MotionMode.CW_ARC).feedRate(feedRate)
                                .XY(currentX, startY)
                                .IJ(offsetCenterX, offsetCenterY)
                                .endLine();
                        break;
                    case COUNTER_CLOCKWISE:
                        builder .motionMode(MotionMode.CCW_ARC).feedRate(feedRate)
                                .XY(currentX, startY)
                                .IJ(offsetCenterX, offsetCenterY)
                                .endLine();
                        break;
                }

                if (currentX > minX) {
                    currentX = Math.max(minX, currentX - stepWidth);

                    builder .motionMode(MotionMode.LINEAR).feedRate(feedRate)
                            .X(currentX).endLine();
                } else {
                    break;
                }
            }

            // update current depth
            currentZ = cutToZ;
        }

        builder .motionMode(MotionMode.RAPID_LINEAR)
                .Z(safetyHeight).endLine();
    }
}
