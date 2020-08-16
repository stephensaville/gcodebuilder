package com.gcodebuilder.recipe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gcodebuilder.generator.GCodeGenerator;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.model.GCodeBuilder;
import com.gcodebuilder.model.LengthUnit;
import com.gcodebuilder.model.UnitMode;
import com.google.common.base.Preconditions;
import lombok.Data;

@Data
public abstract class GCodeRecipe implements Cloneable {
    private int id;
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

    public GCodeRecipe clone() {
        try {
            return (GCodeRecipe)super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public abstract LengthUnit getUnit();

    public abstract void convertToUnit(LengthUnit toUnit);

    public GCodeRecipe getRecipeForUnit(LengthUnit unit) {
        if (getUnit() != unit) {
            GCodeRecipe recipe = clone();
            recipe.convertToUnit(unit);
            return recipe;
        } else {
            return this;
        }
    }

    public abstract GCodeGenerator getGCodeGenerator(Shape<?> shape);

    @Override
    public String toString() {
        return name;
    }
}
