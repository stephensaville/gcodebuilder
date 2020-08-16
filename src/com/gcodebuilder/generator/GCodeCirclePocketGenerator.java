package com.gcodebuilder.generator;

import com.gcodebuilder.geometry.Circle;
import com.gcodebuilder.model.GCodeBuilder;
import com.gcodebuilder.model.MotionMode;
import com.gcodebuilder.recipe.GCodePocketRecipe;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Data
public class GCodeCirclePocketGenerator implements GCodeGenerator {
    private static final Logger log = LogManager.getLogger(GCodeCirclePocketGenerator.class);

    private final GCodePocketRecipe recipe;
    private final Circle shape;

    @Override
    public void generateGCode(GCodeBuilder builder) {
        log.info("Generating pocket gcode for:{}", shape);

        double toolOffset = recipe.getToolWidth() / 2;
        double stepWidth = recipe.getToolWidth() * recipe.getStepOver() / 100.0;

        if (toolOffset > shape.getRadius()) {
            log.warn("Shape:{} is too small to cut pocket using toolWidth:{}", shape, recipe.getToolWidth());
            return;
        }

        // calculate bounds of final (inside profile) cut
        double minX = shape.getMinX() + toolOffset;

        // calculate bounds of starting (center) cut
        double startOffset = Math.max(toolOffset, shape.getRadius() - stepWidth / 2);
        double startX = shape.getMinX() + startOffset;
        double startY = shape.getCenter().getY();

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

            double currentX = startX;

            while (true) {
                double offsetCenterX = shape.getCenter().getX() - currentX;
                double offsetCenterY = 0;

                // cut circular profile
                switch (recipe.getDirection()) {
                    case CLOCKWISE:
                        builder .motionMode(MotionMode.CW_ARC).feedRate(recipe.getFeedRate())
                                .XY(currentX, startY)
                                .IJ(offsetCenterX, offsetCenterY)
                                .endLine();
                        break;
                    case COUNTER_CLOCKWISE:
                        builder .motionMode(MotionMode.CCW_ARC).feedRate(recipe.getFeedRate())
                                .XY(currentX, startY)
                                .IJ(offsetCenterX, offsetCenterY)
                                .endLine();
                        break;
                }

                if (currentX > minX) {
                    currentX = Math.max(minX, currentX - stepWidth);

                    builder .motionMode(MotionMode.LINEAR).feedRate(recipe.getFeedRate())
                            .X(currentX).endLine();
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
