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

import com.gcodebuilder.generator.GCodeDrawable;
import com.gcodebuilder.generator.GCodeDrillingRecipeDrawable;
import com.gcodebuilder.generator.GCodeDrillingRecipeGenerator;
import com.gcodebuilder.generator.GCodeGenerator;
import com.gcodebuilder.geometry.Point;
import com.gcodebuilder.geometry.Shape;

import java.util.List;

public abstract class GCodeDrillingRecipe extends GCodeRecipe {
    public GCodeDrillingRecipe(int id, GCodeRecipeType type) {
        super(id, type);
    }

    @Override
    public GCodeGenerator getGCodeGenerator(Shape<?> shape) {
        return new GCodeDrillingRecipeGenerator(this, shape);
    }

    @Override
    public GCodeDrawable getGCodeDrawable(Shape<?> shape) {
        return new GCodeDrillingRecipeDrawable(this, shape);
    }

    public abstract List<Point> getDrillPoints(Shape<?> shape);
}
