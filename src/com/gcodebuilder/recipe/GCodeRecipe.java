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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gcodebuilder.generator.GCodeGenerator;
import com.gcodebuilder.generator.GCodeRecipeGenerator;
import com.gcodebuilder.generator.toolpath.Toolpath;
import com.gcodebuilder.generator.toolpath.ToolpathGenerator;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.model.GCodeBuilder;
import com.gcodebuilder.model.LengthUnit;
import com.gcodebuilder.model.LengthUnitConverter;
import com.gcodebuilder.model.UnitMode;
import com.google.common.base.Preconditions;
import javafx.scene.canvas.GraphicsContext;
import lombok.Data;

import java.util.List;

@Data
public abstract class GCodeRecipe implements Cloneable {
    private int id;
    private final GCodeRecipeType type;
    private String name;
    private LengthUnit unit = LengthUnit.INCH;
    private double toolWidth = 0.25;
    private double depth = 0.1;
    private double stockSurface = 0;
    private double safetyHeight = 0.5;
    private double stepDown = 0.1;
    private int feedRate = 30;
    private int plungeRate = 30;

    protected GCodeRecipe(int id, GCodeRecipeType type) {
        Preconditions.checkArgument(id > 0, "id must be a positive number");
        Preconditions.checkNotNull(type, "type must be non-null");
        this.id = id;
        this.type = type;
    }

    @JsonCreator
    public static GCodeRecipe create(@JsonProperty("id") int id,
                                     @JsonProperty("type") GCodeRecipeType type) {
        return type.newRecipe(id);
    }

    public GCodeRecipe clone() {
        try {
            return (GCodeRecipe)super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void convertToUnit(LengthUnit toUnit) {
        LengthUnitConverter converter = getUnit().getConverterTo(toUnit);
        setUnit(toUnit);
        setToolWidth(converter.convert(getToolWidth()));
        setDepth(converter.convert(getDepth()));
        setStockSurface(converter.convert(getStockSurface()));
        setSafetyHeight(converter.convert(getSafetyHeight()));
        setStepDown(converter.convert(getStepDown()));
        setFeedRate((int)Math.round(converter.convert(getFeedRate())));
        setPlungeRate((int)Math.round(converter.convert(getPlungeRate())));
    }

    public GCodeRecipe getRecipeForUnit(LengthUnit unit) {
        if (getUnit() != unit) {
            GCodeRecipe recipe = clone();
            recipe.convertToUnit(unit);
            return recipe;
        } else {
            return this;
        }
    }

    public GCodeGenerator getGCodeGenerator(Shape<?> shape) {
        return new GCodeRecipeGenerator(this, shape);
    }

    public abstract List<Toolpath> computeToolpaths(ToolpathGenerator generator, GraphicsContext ctx,
                                                    ToolpathGenerator.DisplayMode displayMode);

    public List<Toolpath> computeToolpaths(ToolpathGenerator generator) {
        return computeToolpaths(generator, null, null);
    }

    @Override
    public String toString() {
        return name;
    }
}
