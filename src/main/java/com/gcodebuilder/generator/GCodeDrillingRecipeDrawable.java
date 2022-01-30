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

import com.gcodebuilder.app.GridSettings;
import com.gcodebuilder.geometry.Point;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.recipe.GCodeDrillingRecipe;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GCodeDrillingRecipeDrawable extends GCodeDrawable {
    private static final Paint PAINT = Color.GREEN;

    private final GCodeDrillingRecipe recipe;
    private final Shape<?> shape;

    @Override
    public void draw(GraphicsContext ctx, double pixelsPerUnit, GridSettings settings) {
        ctx.setLineWidth(settings.getShapeLineWidth() / pixelsPerUnit / 2);
        ctx.setStroke(PAINT);

        double drillWidth = recipe.getToolWidth();
        double drillOffset = drillWidth / 2;
        double crossOffset = drillOffset / Math.sqrt(2.0);
        for (Point drillPoint : recipe.getDrillPoints(shape)) {
            double x = drillPoint.getX();
            double y = drillPoint.getY();
            ctx.strokeOval(x - drillOffset, y - drillOffset, drillWidth, drillWidth);
            ctx.strokeLine(x - crossOffset, y - crossOffset, x + crossOffset, y + crossOffset);
            ctx.strokeLine(x - crossOffset, y + crossOffset, x + crossOffset, y - crossOffset);
        }
    }
}
