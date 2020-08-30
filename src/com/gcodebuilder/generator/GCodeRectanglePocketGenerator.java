package com.gcodebuilder.generator;

import com.gcodebuilder.geometry.Rectangle;
import com.gcodebuilder.recipe.GCodePocketRecipe;

public class GCodeRectanglePocketGenerator extends GCodePathPocketGenerator {
    public GCodeRectanglePocketGenerator(GCodePocketRecipe recipe, Rectangle shape) {
        super(recipe, shape.convertToPath());
    }
}
