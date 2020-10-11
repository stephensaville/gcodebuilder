package com.gcodebuilder.generator.toolpath;

import com.gcodebuilder.app.GridSettings;
import com.gcodebuilder.canvas.Drawable;
import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.recipe.GCodePocketRecipe;
import com.gcodebuilder.recipe.GCodeProfileRecipe;
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
                recipe = drawing.getRecipe(recipeId);
            } else {
                continue;
            }

            // create toolpath generator
            ToolpathGenerator generator = new ToolpathGenerator();
            ctx.setLineWidth(settings.getShapeLineWidth() / pixelsPerUnit / 2);
            generator.setPointRadius(settings.getShapePointRadius() / pixelsPerUnit);
            generator.addAllPaths(shape.convertToPaths());

            // draw toolpaths
            if (recipe instanceof GCodeProfileRecipe) {
                GCodeProfileRecipe profileRecipe = (GCodeProfileRecipe)recipe;
                generator.setToolRadius(profileRecipe.getToolWidth()/2);
                if (displayMode != null && displayMode.compareTo(ToolpathGenerator.DisplayMode.ORIENTED_TOOLPATHS) > 0) {
                    generator.computeProfileToolpaths(profileRecipe.getSide(), profileRecipe.getDirection(),
                            ctx, ToolpathGenerator.DisplayMode.ORIENTED_TOOLPATHS);
                } else {
                    generator.computeProfileToolpaths(profileRecipe.getSide(), profileRecipe.getDirection(),
                            ctx, displayMode);
                }
            } else if (recipe instanceof GCodePocketRecipe) {
                GCodePocketRecipe pocketRecipe = (GCodePocketRecipe)recipe;
                generator.setToolRadius(pocketRecipe.getToolWidth()/2);
                generator.setStepOver(pocketRecipe.getStepOver()/100.0);
                generator.computePocketToolpaths(pocketRecipe.getDirection(), ctx, displayMode);
            }
        }
    }
}
