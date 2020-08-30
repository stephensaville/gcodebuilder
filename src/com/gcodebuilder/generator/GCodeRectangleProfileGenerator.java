package com.gcodebuilder.generator;

import com.gcodebuilder.geometry.Rectangle;
import com.gcodebuilder.recipe.GCodeProfileRecipe;

public class GCodeRectangleProfileGenerator extends GCodePathProfileGenerator {
    public GCodeRectangleProfileGenerator(GCodeProfileRecipe recipe, Rectangle shape) {
        super(recipe, shape.convertToPath());
    }
}
