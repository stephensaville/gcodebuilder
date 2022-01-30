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

import com.gcodebuilder.generator.GCodeDisplayMode;
import com.gcodebuilder.generator.GCodeDrawable;
import com.gcodebuilder.generator.GCodeGenerator;
import com.gcodebuilder.generator.GCodeToolpathRecipeDrawable;
import com.gcodebuilder.generator.GCodeToolpathRecipeGenerator;
import com.gcodebuilder.generator.toolpath.Toolpath;
import com.gcodebuilder.generator.toolpath.ToolpathGenerator;
import com.gcodebuilder.geometry.Shape;
import javafx.scene.canvas.GraphicsContext;

import java.util.List;

public abstract class GCodeToolpathRecipe extends GCodeRecipe {
    public GCodeToolpathRecipe(int id, GCodeRecipeType type) {
        super(id, type);
    }

    @Override
    public GCodeGenerator getGCodeGenerator(Shape<?> shape) {
        return new GCodeToolpathRecipeGenerator(this, shape);
    }

    @Override
    public GCodeDrawable getGCodeDrawable(Shape<?> shape) {
        return new GCodeToolpathRecipeDrawable(this, shape);
    }

    public abstract List<Toolpath> computeToolpaths(ToolpathGenerator generator, GraphicsContext ctx,
                                                    GCodeDisplayMode displayMode);

    public List<Toolpath> computeToolpaths(ToolpathGenerator generator) {
        return computeToolpaths(generator, null, null);
    }
}
