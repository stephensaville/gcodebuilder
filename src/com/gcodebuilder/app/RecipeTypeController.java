package com.gcodebuilder.app;

import com.gcodebuilder.recipe.GCodeRecipe;

public interface RecipeTypeController {
    void setRecipe(GCodeRecipe recipe);
    GCodeRecipe getRecipe();
}
