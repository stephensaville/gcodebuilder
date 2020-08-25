package com.gcodebuilder.generator;

import com.gcodebuilder.geometry.Rectangle;
import com.gcodebuilder.recipe.GCodeProfileRecipe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GCodeRectangleProfileGenerator extends GCodePathProfileGenerator {
    private static final Logger log = LogManager.getLogger(GCodeRectangleProfileGenerator.class);

    public GCodeRectangleProfileGenerator(GCodeProfileRecipe recipe, Rectangle shape) {
        super(recipe, shape.convertToPath());
    }
}
