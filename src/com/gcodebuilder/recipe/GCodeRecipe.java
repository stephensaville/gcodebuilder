package com.gcodebuilder.recipe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.model.GCodeBuilder;
import com.google.common.base.Preconditions;
import lombok.Data;

@Data
public abstract class GCodeRecipe {
    private final int id;
    private final GCodeRecipeType type;
    private String name;

    protected GCodeRecipe(int id, GCodeRecipeType type) {
        Preconditions.checkArgument(id > 0, "id must be a positive number");
        Preconditions.checkNotNull(type, "type must be non-null");
        this.id = id;
        this.type = type;
    }

    @JsonCreator
    public static GCodeRecipe create(@JsonProperty("id") int id,
                                     @JsonProperty("type") GCodeRecipeType type) {
        return type.newRecipe(id);
    }

    public abstract void generateGCode(Shape<?> shape, GCodeBuilder builder);

    @Override
    public String toString() {
        return name;
    }
}
