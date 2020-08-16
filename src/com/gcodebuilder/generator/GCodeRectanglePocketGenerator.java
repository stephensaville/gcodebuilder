package com.gcodebuilder.generator;

import com.gcodebuilder.geometry.Rectangle;
import com.gcodebuilder.model.DistanceMode;
import com.gcodebuilder.model.FeedRateMode;
import com.gcodebuilder.model.GCodeBuilder;
import com.gcodebuilder.model.MotionMode;
import com.gcodebuilder.recipe.GCodePocketRecipe;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Data
public class GCodeRectanglePocketGenerator implements GCodeGenerator {
    private static final Logger log = LogManager.getLogger(GCodeRectanglePocketGenerator.class);

    private final GCodePocketRecipe recipe;
    private final Rectangle shape;

    @Override
    public void generateGCode(GCodeBuilder builder) {
        log.info("Generating pocket gcode for:{}", shape);

        double shortDimension = Math.min(shape.getWidth(), shape.getHeight());
        if (recipe.getToolWidth() > shortDimension) {
            log.warn("Shape:{} is too small to cut pocket using toolWidth:{}", shape, recipe.getToolWidth());
            return;
        }

        double toolOffset = recipe.getToolWidth() / 2;
        double stepWidth = recipe.getToolWidth() * recipe.getStepOver() / 100.0;

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

        double currentZ = recipe.getStockSurface();
        double minZ = recipe.getStockSurface() - recipe.getDepth();

        builder .distanceMode(DistanceMode.ABSOLUTE)
                .feedRateMode(FeedRateMode.UNITS_PER_MIN);

        while (currentZ > minZ) {
            // step down or bottom out
            double cutToZ = Math.max(minZ, currentZ - recipe.getStepDown());

            // move over starting point
            builder .motionMode(MotionMode.RAPID_LINEAR)
                    .Z(recipe.getSafetyHeight()).endLine()
                    .XY(startMinX, startMinY).endLine();

            // plunge down to cut depth
            builder .motionMode(MotionMode.LINEAR).feedRate(recipe.getPlungeRate())
                    .Z(cutToZ).endLine()
                    .feedRate(recipe.getFeedRate());

            double currentMinY = startMinY;
            double currentMinX = startMinX;
            double currentMaxY = startMaxY;
            double currentMaxX = startMaxX;

            // cut pocket in XY plane
            while (true) {
                // cut around current rectangle
                switch (recipe.getDirection()) {
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
                .Z(recipe.getSafetyHeight()).endLine();
    }
}
