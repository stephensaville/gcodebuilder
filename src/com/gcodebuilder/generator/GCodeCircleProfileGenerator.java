package com.gcodebuilder.generator;

import com.gcodebuilder.geometry.Circle;
import com.gcodebuilder.model.ArcDistanceMode;
import com.gcodebuilder.model.DistanceMode;
import com.gcodebuilder.model.FeedRateMode;
import com.gcodebuilder.model.GCodeBuilder;
import com.gcodebuilder.model.MotionMode;
import com.gcodebuilder.recipe.GCodeProfileRecipe;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Data
public class GCodeCircleProfileGenerator implements GCodeGenerator {
    private static final Logger log = LogManager.getLogger(GCodeCircleProfileGenerator.class);

    private final GCodeProfileRecipe recipe;
    private final Circle shape;

    @Override
    public void generateGCode(GCodeBuilder builder) {
        log.info("Generating GCode for:{}", shape);

        builder .distanceMode(DistanceMode.ABSOLUTE)
                .arcDistanceMode(ArcDistanceMode.INCREMENTAL)
                .feedRateMode(FeedRateMode.UNITS_PER_MIN);

        double toolOffset = recipe.getToolWidth()/2 * recipe.getSide().getOffsetSign();

        double startX = shape.getMinX() - toolOffset;
        double startY = shape.getCenter().getY();
        double offsetCenterX = shape.getCenter().getX() - startX;
        double offsetCenterY = 0;

        double currentZ = recipe.getStockSurface();
        double minZ = recipe.getStockSurface() - recipe.getDepth();

        while (currentZ > minZ) {
            // step down or bottom out
            double cutToZ = Math.max(minZ, currentZ - recipe.getStepDown());

            // move over starting point
            builder .motionMode(MotionMode.RAPID_LINEAR)
                    .Z(recipe.getSafetyHeight()).endLine()
                    .XY(startX, startY).endLine();

            // plunge down to cut depth
            builder .motionMode(MotionMode.LINEAR).feedRate(recipe.getPlungeRate())
                    .Z(cutToZ).endLine();

            // cut circular profile
            switch (recipe.getDirection()) {
                case CLOCKWISE:
                    builder.motionMode(MotionMode.CW_ARC).feedRate(recipe.getFeedRate())
                            .XY(startX, startY)
                            .IJ(offsetCenterX, offsetCenterY)
                            .endLine();
                    break;
                case COUNTER_CLOCKWISE:
                    builder.motionMode(MotionMode.CCW_ARC).feedRate(recipe.getFeedRate())
                            .XY(startX, startY)
                            .IJ(offsetCenterX, offsetCenterY)
                            .endLine();
                    break;
            }

            // update current depth
            currentZ = cutToZ;
        }

        builder .motionMode(MotionMode.RAPID_LINEAR)
                .Z(recipe.getSafetyHeight()).endLine();
    }
}
