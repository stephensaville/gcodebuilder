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

package com.gcodebuilder.generator.toolpath;

import com.gcodebuilder.app.GridSettings;
import com.gcodebuilder.canvas.Drawable;
import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.recipe.GCodeRecipe;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import lombok.Data;

@Data
public class ToolpathDrawable implements Drawable {
    private ToolpathGenerator.DisplayMode displayMode;
    private Drawing drawing;

    @Override
    public boolean isVisible() {
        return displayMode != null && drawing != null;
    }

    @Override
    public void draw(GraphicsContext ctx, double pixelsPerUnit, GridSettings settings) {
        if (!isVisible()) {
            return;
        }

        ctx.setFont(Font.font(10.0));

        for (Shape<?> shape : drawing.getShapes()) {
            // get shape recipe
            int recipeId = shape.getRecipeId();
            GCodeRecipe recipe;
            if (recipeId > 0) {
                recipe = drawing.getRecipe(recipeId).getRecipeForUnit(drawing.getLengthUnit());
            } else {
                continue;
            }

            // create toolpath generator
            ToolpathGenerator generator = new ToolpathGenerator();
            ctx.setLineWidth(settings.getShapeLineWidth() / pixelsPerUnit / 2);
            generator.setPointRadius(settings.getShapePointRadius() / pixelsPerUnit);
            generator.setToolRadius(recipe.getToolWidth() / 2);
            generator.addAllPaths(shape.convertToPaths());

            // compute (and draw) toolpaths
            recipe.computeToolpaths(generator, ctx, displayMode);
        }
    }
}
