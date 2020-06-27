package com.gcodebuilder.recipe;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GCodeRecipeType {
    PROFILE("Profile", GCodeProfileRecipe.class) {
        @Override
        public GCodeRecipe newRecipe(int id) {
            return new GCodeProfileRecipe(id);
        }
    },
    POCKET("Pocket", GCodePocketRecipe.class) {
        @Override
        public GCodeRecipe newRecipe(int id) {
            return new GCodePocketRecipe(id);
        }
    };

    private final String label;
    private final Class<? extends GCodeRecipe> recipeClass;

    public abstract GCodeRecipe newRecipe(int id);
}
