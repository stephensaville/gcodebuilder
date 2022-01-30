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

package com.gcodebuilder.generator;

import com.gcodebuilder.geometry.Point;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.model.ArcDistanceMode;
import com.gcodebuilder.model.DistanceMode;
import com.gcodebuilder.model.FeedRateMode;
import com.gcodebuilder.model.GCodeBuilder;
import com.gcodebuilder.model.MotionMode;
import com.gcodebuilder.recipe.GCodeDrillingRecipe;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Data
public class GCodeDrillingRecipeGenerator implements GCodeGenerator {
    private static final Logger log = LogManager.getLogger(GCodeDrillingRecipeGenerator.class);

    private final GCodeDrillingRecipe recipe;
    private final Shape<?> shape;

    public GCodeDrillingRecipeGenerator(GCodeDrillingRecipe recipe, Shape<?> shape) {
        this.recipe = recipe;
        this.shape = shape;
    }

    @Override
    public void generateGCode(GCodeBuilder builder) {
        log.info("Generating GCode for:{}", shape);

        builder .distanceMode(DistanceMode.ABSOLUTE)
                .arcDistanceMode(ArcDistanceMode.INCREMENTAL)
                .feedRateMode(FeedRateMode.UNITS_PER_MIN);

        for (Point drillPoint : recipe.getDrillPoints(shape)) {
            // move to drill point
            builder.motionMode(MotionMode.RAPID_LINEAR)
                    .Z(recipe.getSafetyHeight()).endLine()
                    .XY(drillPoint.getX(), drillPoint.getY()).endLine();

            double currentZ = recipe.getStockSurface();
            double minZ = recipe.getStockSurface() - recipe.getDepth();

            while (currentZ > minZ) {
                // step down or bottom out
                double cutToZ = Math.max(minZ, currentZ - recipe.getStepDown());

                // drill down
                builder.motionMode(MotionMode.LINEAR).feedRate(recipe.getPlungeRate())
                        .Z(cutToZ).endLine();

                // pull out
                builder.motionMode(MotionMode.RAPID_LINEAR)
                        .Z(recipe.getSafetyHeight()).endLine();

                // update current depth
                currentZ = cutToZ;
            }
        }
    }
}
