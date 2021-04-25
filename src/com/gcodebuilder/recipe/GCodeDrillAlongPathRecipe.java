/*
 * Copyright (c) 2021 Stephen Saville
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gcodebuilder.recipe;

import com.gcodebuilder.generator.toolpath.Toolpath;
import com.gcodebuilder.generator.toolpath.ToolpathGenerator;
import com.gcodebuilder.geometry.Math2D;
import com.gcodebuilder.geometry.PathSegment;
import com.gcodebuilder.geometry.Point;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.model.Direction;
import com.gcodebuilder.model.HoleAlignment;
import com.gcodebuilder.model.HoleSpacingMode;
import javafx.geometry.Point2D;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@Setter
public class GCodeDrillAlongPathRecipe extends GCodeDrillingRecipe {
    private static final Logger log = LogManager.getLogger(GCodeDrillAlongPathRecipe.class);

    private Direction direction = Direction.ORIGINAL;
    private HoleSpacingMode holeSpacingMode = HoleSpacingMode.EDGE;
    private double holeSpacing;
    private HoleAlignment holeAlignment = HoleAlignment.START;
    private double holeOffset;

    public GCodeDrillAlongPathRecipe(int id) {
        super(id, GCodeRecipeType.DRILL_ALONG_PATH);
        setName(String.format("pathDrill%d", id));
    }

    private static List<Point> getDrillPointsOnPathFromStart(Toolpath path, double centerToCenter, double holeOffset) {
        List<Point> points = new ArrayList<>();
        Point2D prevPoint = null;
        double offset = holeOffset;
        for (Toolpath.Segment toolpathSegment : path.getSegments()) {
            PathSegment pathSegment = toolpathSegment.getSegment();
            Optional<Point2D> nextPoint;
            if (prevPoint == null) {
                prevPoint = pathSegment.getFrom();
                nextPoint = pathSegment.nextPointOnPath(prevPoint, offset, true);
            } else {
                nextPoint = pathSegment.nextPointOnPath(prevPoint, offset, false);
            }
            while (nextPoint.isPresent()) {
                offset = centerToCenter;
                prevPoint = nextPoint.get();
                points.add(new Point(prevPoint));
                nextPoint = pathSegment.nextPointOnPath(prevPoint, offset, true);
            }
        }
        return points;
    }

    private static List<Point> getDrillPointsOnPathFromEnd(Toolpath path, double centerToCenter, double holeOffset) {
        List<Point> points = getDrillPointsOnPathFromStart(path.reverse(), centerToCenter, holeOffset);
        Collections.reverse(points);
        return points;
    }

    private static List<Point> getDrillPointsOnPath(Toolpath path, double centerToCenter,
                                                    HoleAlignment holeAlignment, double holeOffset) {
        List<Point> points;
        if (holeAlignment == HoleAlignment.END) {
            // end-alignment is equivalent to starting from end and going backwards
            points = getDrillPointsOnPathFromEnd(path, centerToCenter, holeOffset);
        } else if (holeAlignment == HoleAlignment.MIDDLE) {
            double totalLength = path.getSegments().stream().mapToDouble(s -> s.getSegment().getLength()).sum();
            double halfLength = totalLength / 2;
            double currentLength = 0;
            List<Toolpath.Segment> segmentsBefore = new ArrayList<>();
            List<Toolpath.Segment> segmentsAfter = new ArrayList<>();
            for (Toolpath.Segment segment : path.getSegments()) {
                double segmentLength = segment.getSegment().getLength();
                if (currentLength > halfLength) {
                    // after middle
                    segmentsAfter.add(segment);
                } else if (currentLength + segmentLength > halfLength) {
                    // at middle
                    Point2D middle = segment.getSegment().pointOnSegment(halfLength - currentLength);
                    PathSegment.SplitSegments splitSegments = segment.getSegment().split(middle);
                    Toolpath.Connection connection = new Toolpath.Connection(middle);
                    Toolpath.Segment segmentBefore = new Toolpath.Segment(
                            splitSegments.getFromSegment(), segment.getToolRadius(), segment.isLeftSide(),
                            segment.getFromConnection(), connection);
                    segmentsBefore.add(segmentBefore);
                    Toolpath.Segment segmentAfter = new Toolpath.Segment(
                            splitSegments.getToSegment(), segment.getToolRadius(), segment.isLeftSide(),
                            connection, segment.getToConnection());
                    segmentsAfter.add(segmentAfter);
                } else {
                    // before middle
                    segmentsBefore.add(segment);
                }
                currentLength += segmentLength;
            }
            Toolpath pathBefore = new Toolpath(segmentsBefore);
            points = getDrillPointsOnPathFromEnd(pathBefore, centerToCenter, centerToCenter - holeOffset);
            Toolpath pathAfter = new Toolpath(segmentsAfter);
            points.addAll(getDrillPointsOnPathFromStart(pathAfter, centerToCenter, holeOffset));
        } else {
            points = getDrillPointsOnPathFromStart(path, centerToCenter, holeOffset);
        }
        if (points.size() > 1) {
            // remove last point if too close to first
            Point firstPoint = points.get(0);
            Point lastPoint = points.get(points.size() - 1);
            if (firstPoint.distance(lastPoint) < centerToCenter) {
                if (holeAlignment == HoleAlignment.END) {
                    // remove first point when starting from end
                    points.remove(0);
                } else {
                    // otherwise remove last point
                    points.remove(points.size() - 1);
                }
            }
        }
        return points;
    }

    @Override
    public List<Point> getDrillPoints(Shape<?> shape) {
        final double centerToCenter;
        switch (holeSpacingMode) {
            case CENTER:
                centerToCenter = Math.max(holeSpacing, getToolWidth() / 2);
                break;
            case EDGE:
                centerToCenter = holeSpacing + getToolWidth();
                break;
            default:
                throw new IllegalArgumentException(String.format(
                        "hole spacing mode not supported: %s", holeSpacingMode));
        }
        ToolpathGenerator generator = new ToolpathGenerator();
        generator.setToolRadius(getToolWidth() / 2);
        generator.addAllPaths(shape.convertToPaths());
        List<Toolpath> paths = generator.computeFollowPathToolpaths(getDirection());
        return paths.stream()
                .flatMap(path -> getDrillPointsOnPath(path, centerToCenter, holeAlignment, holeOffset).stream())
                .collect(Collectors.toList());
    }
}
