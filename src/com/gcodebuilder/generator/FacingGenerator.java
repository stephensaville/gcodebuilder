package com.gcodebuilder.generator;

import com.gcodebuilder.model.*;
import lombok.Data;

@Data
public class FacingGenerator implements GCodeGenerator {
    private LengthUnit unit = LengthUnit.INCH;
    private double toolWidth = 0.25;
    private double x = 0;
    private double y = 0;
    private double width = 1;
    private double length = 1;
    private double depth = 0.1;
    private double stockSurface = 0;
    private double safetyHeight = 0.5;
    private double stepDown = 0.1;
    private double stepOver = 40;
    private double borderWidth = 15;
    private int feedRate = 30;
    private int plungeRate = 30;

    @Override
    public void generateGCode(GCodeBuilder builder) {
        builder.unitMode(unit.getMode())
                .distanceMode(DistanceMode.ABSOLUTE)
                .feedRateMode(FeedRateMode.UNITS_PER_MIN)
                .endLine();

        // spindle bounds
        double minX = x + toolWidth*(0.5 - borderWidth);
        double maxX = x + width - toolWidth*(50 - borderWidth)/100.0;
        double centerX = (minX + maxX) / 2;
        double minY = y + toolWidth*(0.5 - borderWidth);
        double maxY = y + length - toolWidth*(50 - borderWidth)/100.0;
        double centerY = (minY + maxY) / 2;

        double currentZ = stockSurface;
        double minZ = stockSurface - depth;

        while (currentZ > minZ) {
            currentZ = Math.max(minZ, currentZ - stepDown);

            builder.motionMode(MotionMode.RAPID_LINEAR)
                    .Z(stockSurface + safetyHeight).endLine()
                    .XY(minX, minY).endLine();
            builder.motionMode(MotionMode.LINEAR).feedRate(plungeRate)
                    .Z(currentZ).endLine()
                    .feedRate(feedRate);

            double stepAdjustment = 0;
            double maxStepAdjustment = Math.min(centerX - minX, centerY - minY);

            while (stepAdjustment < maxStepAdjustment) {
                double fromX = Math.min(minX + stepAdjustment, centerX);
                double toX = Math.max(maxX - stepAdjustment, centerX);
                double fromY = Math.min(minY + stepAdjustment, centerY);
                double toY = Math.max(maxY - stepAdjustment, centerY);

                builder
                        .XY(fromX, fromY).endLine()
                        .Y(toY).endLine()
                        .X(toX).endLine()
                        .Y(fromY).endLine()
                        .X(fromX).endLine();

                stepAdjustment += toolWidth*stepOver/100.0;
            }
        }

        builder.motionMode(MotionMode.RAPID_LINEAR)
                .Z(stockSurface + safetyHeight).endLine();
    }
}
