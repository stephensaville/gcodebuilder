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

import com.gcodebuilder.generator.GCodeFollowPathGenerator;
import com.gcodebuilder.generator.GCodeGenerator;
import com.gcodebuilder.generator.GCodePathPocketGenerator;
import com.gcodebuilder.generator.toolpath.Toolpath;
import com.gcodebuilder.generator.toolpath.ToolpathGenerator;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.model.Direction;
import com.gcodebuilder.model.LengthUnit;
import com.gcodebuilder.model.LengthUnitConverter;
import javafx.scene.canvas.GraphicsContext;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@Getter
@Setter
public class GCodeFollowPathRecipe extends GCodeRecipe {
    private static final Logger log = LogManager.getLogger(GCodeFollowPathRecipe.class);

    private LengthUnit unit = LengthUnit.INCH;
    private double toolWidth = 0.25;
    private double depth = 0.1;
    private double stockSurface = 0;
    private double safetyHeight = 0.5;
    private double stepDown = 0.1;
    private int feedRate = 30;
    private int plungeRate = 30;
    private Direction direction = Direction.ORIGINAL;

    public GCodeFollowPathRecipe(int id) {
        super(id, GCodeRecipeType.FOLLOW_PATH);
        setName(String.format("followPath%d", id));
    }

    @Override
    public GCodeFollowPathRecipe clone() {
        return (GCodeFollowPathRecipe)super.clone();
    }

    @Override
    public void convertToUnit(LengthUnit toUnit) {
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
    public GCodeGenerator getGCodeGenerator(Shape<?> shape) {
        return new GCodeFollowPathGenerator(this, shape);
    }

    @Override
    public List<Toolpath> computeToolpaths(ToolpathGenerator generator, GraphicsContext ctx,
                                           ToolpathGenerator.DisplayMode displayMode) {
        generator.setToolRadius(getToolWidth()/2);
        return generator.computeFollowPathToolpaths(getDirection(), ctx, displayMode);
    }
}
