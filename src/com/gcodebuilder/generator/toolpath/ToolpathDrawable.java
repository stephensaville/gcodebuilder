package com.gcodebuilder.generator.toolpath;

import com.gcodebuilder.app.GridSettings;
import com.gcodebuilder.canvas.Drawable;
import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Path;
import com.gcodebuilder.geometry.PathConvertible;
import com.gcodebuilder.geometry.PathGroup;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.model.Side;
import com.gcodebuilder.recipe.GCodePocketRecipe;
import com.gcodebuilder.recipe.GCodeProfileRecipe;
import com.gcodebuilder.recipe.GCodeRecipe;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import lombok.Data;

import java.util.Collections;
import java.util.List;

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
                recipe = drawing.getRecipe(recipeId);
            } else {
                continue;
            }

            // convert shape to paths
            List<Path> paths;
            if (shape instanceof PathConvertible) {
                paths = Collections.singletonList(((PathConvertible)shape).convertToPath());
            } else if (shape instanceof PathGroup) {
                paths = ((PathGroup)shape).getPaths();
            } else {
                continue;
            }

            // convert toolpath generator
            ToolpathGenerator generator = new ToolpathGenerator();
            ctx.setLineWidth(settings.getShapeLineWidth() / pixelsPerUnit / 2);
            generator.setPointRadius(settings.getShapePointRadius() / pixelsPerUnit);
            generator.addAllPaths(paths);
            if (recipe instanceof GCodeProfileRecipe) {
                GCodeProfileRecipe profileRecipe = (GCodeProfileRecipe)recipe;
                generator.setToolRadius(profileRecipe.getToolWidth()/2);
                generator.computeProfileToolpaths(profileRecipe.getSide(), profileRecipe.getDirection(),
                        ctx, displayMode);
            } else if (recipe instanceof GCodePocketRecipe) {
                GCodePocketRecipe pocketRecipe = (GCodePocketRecipe)recipe;
                generator.setToolRadius(pocketRecipe.getToolWidth()/2);
                generator.setStepOver(pocketRecipe.getStepOver()/100.0);
                generator.computePocketToolpaths(pocketRecipe.getDirection(), ctx, displayMode);
            }
        }
    }
}
