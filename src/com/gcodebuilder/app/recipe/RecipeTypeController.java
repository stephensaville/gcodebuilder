package com.gcodebuilder.app.recipe;

import com.gcodebuilder.recipe.GCodeRecipe;

public interface RecipeTypeController {
    void setRecipe(GCodeRecipe recipe);
    GCodeRecipe getRecipe();
}
