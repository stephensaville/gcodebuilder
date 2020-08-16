package com.gcodebuilder.generator;

import com.gcodebuilder.geometry.Rectangle;
import com.gcodebuilder.model.DistanceMode;
import com.gcodebuilder.model.FeedRateMode;
import com.gcodebuilder.model.GCodeBuilder;
import com.gcodebuilder.model.MotionMode;
import com.gcodebuilder.recipe.GCodeProfileRecipe;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Data
public class GCodeRectangleProfileGenerator implements GCodeGenerator {
    private static final Logger log = LogManager.getLogger(GCodeRectangleProfileGenerator.class);

    private final GCodeProfileRecipe recipe;
    private final Rectangle shape;

    @Override
    public void generateGCode(GCodeBuilder builder) {
        log.info("Generating GCode for:{}", shape);

        builder .distanceMode(DistanceMode.ABSOLUTE)
                .feedRateMode(FeedRateMode.UNITS_PER_MIN);

        double toolOffset = recipe.getToolWidth()/2 * recipe.getSide().getOffsetSign();

        double minX = shape.getMinX() - toolOffset;
        double minY = shape.getMinY() - toolOffset;
        double maxX = shape.getMaxX() + toolOffset;
        double maxY = shape.getMaxY() + toolOffset;

        double currentZ = recipe.getStockSurface();
        double minZ = recipe.getStockSurface() - recipe.getDepth();

        while (currentZ > minZ) {
            // step down or bottom out
            double cutToZ = Math.max(minZ, currentZ - recipe.getStepDown());

            // move over starting point
            builder .motionMode(MotionMode.RAPID_LINEAR)
                    .Z(recipe.getSafetyHeight()).endLine()
                    .XY(minX, minY).endLine();

            // plunge down to cut depth
            builder .motionMode(MotionMode.LINEAR).feedRate(recipe.getPlungeRate())
                    .Z(cutToZ).endLine()
                    .feedRate(recipe.getFeedRate());

            // cut profile in XY plane
            switch (recipe.getDirection()) {
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
                .Z(recipe.getSafetyHeight()).endLine();
    }
}
