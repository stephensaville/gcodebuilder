package com.gcodebuilder.generator;

import com.gcodebuilder.generator.toolpath.Toolpath;
import com.gcodebuilder.generator.toolpath.ToolpathGenerator;
import com.gcodebuilder.geometry.Math2D;
import com.gcodebuilder.geometry.Path;
import com.gcodebuilder.geometry.UnitVector;
import com.gcodebuilder.model.ArcDistanceMode;
import com.gcodebuilder.model.DistanceMode;
import com.gcodebuilder.model.FeedRateMode;
import com.gcodebuilder.model.GCodeBuilder;
import com.gcodebuilder.model.MotionMode;
import com.gcodebuilder.recipe.GCodePocketRecipe;
import javafx.geometry.Point2D;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@Data
public class GCodePathPocketGenerator implements GCodeGenerator {
    private static final Logger log = LogManager.getLogger(GCodePathPocketGenerator.class);

    private final GCodePocketRecipe recipe;
    private final Path shape;

    @Override
    public void generateGCode(GCodeBuilder builder) {
        log.info("Generating GCode for:{}", shape);

        ToolpathGenerator generator = new ToolpathGenerator();
        generator.setToolRadius(recipe.getToolWidth()/2);
        generator.addPath(shape);
        List<Toolpath> toolpaths = generator.computePocketToolpaths(recipe.getDirection());

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
                        // join segments with corner arc
                        Point2D arcCenter = segment.getFromConnection().getConnectionPoint();

                        // determine if arc is CW or CCW
                        UnitVector centerToStart = UnitVector.from(arcCenter, currentPoint);
                        UnitVector centerToEnd = UnitVector.from(arcCenter, segment.getFrom());
                        double arcAngle = Math2D.subtractAngle(centerToEnd.getAngle(), centerToStart.getAngle());
                        MotionMode arcMode = (arcAngle > 0) ? MotionMode.CCW_ARC : MotionMode.CW_ARC;

                        builder.motionMode(arcMode).feedRate(recipe.getFeedRate())
                                .XY(segment.getFrom().getX(), segment.getFrom().getY())
                                .IJ(arcCenter.getX() - currentPoint.getX(), arcCenter.getY() - currentPoint.getY())
                                .endLine();
                    }

                    builder.motionMode(MotionMode.LINEAR).feedRate(recipe.getFeedRate())
                            .XY(segment.getTo().getX(), segment.getTo().getY())
                            .endLine();

                    currentPoint = segment.getTo();
                }

                // move between connected toolpaths
                if (toolpath.hasNext()) {
                    Point2D nextStartPoint = toolpath.getNext().getLastSegment().getTo();
                    builder.XY(nextStartPoint.getX(), nextStartPoint.getY()).endLine();
                    currentPoint = nextStartPoint;
                }
            }

            // update current depth
            currentZ = cutToZ;
        }

        builder .motionMode(MotionMode.RAPID_LINEAR)
                .Z(recipe.getSafetyHeight()).endLine();
    }
}
