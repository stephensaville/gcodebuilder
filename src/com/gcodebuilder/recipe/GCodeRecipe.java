package com.gcodebuilder.recipe;

import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.model.GCodeBuilder;
import lombok.Data;

@Data
public abstract class GCodeRecipe {
    private final int id;
    private final GCodeRecipeType type;
    private String name;

    public abstract void generateGCode(Shape<?> shape, GCodeBuilder builder);

    @Override
    public String toString() {
        return name;
    }
}
