package com.gcodebuilder.app.recipe;

import com.gcodebuilder.recipe.GCodeRecipe;

import java.util.function.Consumer;

public interface RecipeTypeController {
    void setRecipe(GCodeRecipe recipe);
    GCodeRecipe getRecipe();

    void setOnRecipeUpdate(Consumer<GCodeRecipe> onRecipeUpdate);
    Consumer<GCodeRecipe> getOnRecipeUpdate();
}
