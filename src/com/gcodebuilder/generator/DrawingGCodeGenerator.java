package com.gcodebuilder.generator;

import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.model.ArcDistanceMode;
import com.gcodebuilder.model.DistanceMode;
import com.gcodebuilder.model.FeedRateMode;
import com.gcodebuilder.model.GCodeBuilder;
import com.gcodebuilder.recipe.GCodeRecipe;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Data
public class DrawingGCodeGenerator implements GCodeGenerator {
    private static final Logger log = LogManager.getLogger(DrawingGCodeGenerator.class);

    private final Drawing drawing;

    @Override
    public void generateGCode(GCodeBuilder builder) {
        builder .unitMode(drawing.getLengthUnit().getMode())
                .distanceMode(DistanceMode.ABSOLUTE)
                .arcDistanceMode(ArcDistanceMode.INCREMENTAL)
                .feedRateMode(FeedRateMode.UNITS_PER_MIN);

        for (Shape<?> shape : drawing.getShapes()) {
            int recipeId = shape.getRecipeId();
            if (recipeId <= 0) {
                continue;
            }

            GCodeRecipe recipe = drawing.getRecipe(recipeId).getRecipeForUnit(drawing.getLengthUnit());
            GCodeGenerator generator = recipe.getGCodeGenerator(shape);
            builder.emptyLine();
            if (generator != null) {
                builder.comment(String.format("shape:%s recipe:%s",
                        shape.getClass().getSimpleName(), recipe.getName()));
                generator.generateGCode(builder);
            } else {
                log.warn("Recipe:{} returned null generator for shape:{}", recipe, shape);
                builder.comment(String.format("shape:%s recipe:%s - no generator available",
                        shape.getClass().getSimpleName(), recipe.getName()));
            }
        }
    }
}
