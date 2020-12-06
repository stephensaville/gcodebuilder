package com.gcodebuilder.generator;

import com.gcodebuilder.generator.toolpath.Toolpath;
import com.gcodebuilder.generator.toolpath.ToolpathGenerator;
import com.gcodebuilder.geometry.ArcSegment;
import com.gcodebuilder.geometry.Math2D;
import com.gcodebuilder.geometry.Path;
import com.gcodebuilder.geometry.UnitVector;
import com.gcodebuilder.model.ArcDistanceMode;
import com.gcodebuilder.model.DistanceMode;
import com.gcodebuilder.model.FeedRateMode;
import com.gcodebuilder.model.GCodeBuilder;
import com.gcodebuilder.model.MotionMode;
import com.gcodebuilder.recipe.GCodeProfileRecipe;
import javafx.geometry.Point2D;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

@Data
public class GCodePathProfileGenerator implements GCodeGenerator {
    private static final Logger log = LogManager.getLogger(GCodePathProfileGenerator.class);

    private final GCodeProfileRecipe recipe;
    private final List<Path> paths;

    public GCodePathProfileGenerator(GCodeProfileRecipe recipe, Path... paths) {
        this.recipe = recipe;
        this.paths = Arrays.asList(paths);
    }

    public GCodePathProfileGenerator(GCodeProfileRecipe recipe, List<Path> paths) {
        this.recipe = recipe;
        this.paths = paths;
    }

    @Override
    public void generateGCode(GCodeBuilder builder) {
        log.info("Generating GCode for:{}", paths);

        ToolpathGenerator generator = new ToolpathGenerator();
        generator.setToolRadius(recipe.getToolWidth()/2);
        generator.addAllPaths(paths);
        List<Toolpath> toolpaths = generator.computeProfileToolpaths(
                recipe.getSide(), recipe.getDirection());

        builder .distanceMode(DistanceMode.ABSOLUTE)
                .arcDistanceMode(ArcDistanceMode.INCREMENTAL)
                .feedRateMode(FeedRateMode.UNITS_PER_MIN);

        double currentZ = recipe.getStockSurface();
        double minZ = recipe.getStockSurface() - recipe.getDepth();

        while (currentZ > minZ) {
            // step down or bottom out
            double cutToZ = Math.max(minZ, currentZ - recipe.getStepDown());

            Point2D currentPoint = null;
            for (Toolpath toolpath : toolpaths) {
                // move to start point (unless already at start point)
                Point2D startPoint = toolpath.getLastSegment().getTo();
                if (!ToolpathGenerator.isSamePoint(currentPoint, startPoint)) {
                    // move over starting point
                    builder.motionMode(MotionMode.RAPID_LINEAR)
                            .Z(recipe.getSafetyHeight()).endLine()
                            .XY(startPoint.getX(), startPoint.getY()).endLine();

                    // plunge down to cut depth
                    builder.motionMode(MotionMode.LINEAR).feedRate(recipe.getPlungeRate())
                            .Z(cutToZ).endLine();
                }
                currentPoint = startPoint;

                // cut profile in XY plane
                for (Toolpath.Segment segment : toolpath.getSegments()) {
                    if (!ToolpathGenerator.isSamePoint(currentPoint, segment.getFrom())) {
                        throw new IllegalStateException("toolpath segments are not connected!");
                    }

                    segment.generateGCode(builder, recipe.getFeedRate());

                    currentPoint = segment.getTo();
                }
            }

            // update current depth
            currentZ = cutToZ;
        }

        builder .motionMode(MotionMode.RAPID_LINEAR)
                .Z(recipe.getSafetyHeight()).endLine();
    }
}
