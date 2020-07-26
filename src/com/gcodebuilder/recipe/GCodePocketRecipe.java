package com.gcodebuilder.recipe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gcodebuilder.geometry.Circle;
import com.gcodebuilder.geometry.Rectangle;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.model.Direction;
import com.gcodebuilder.model.GCodeBuilder;
import com.gcodebuilder.model.LengthUnit;
import com.gcodebuilder.model.Side;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GCodePocketRecipe extends GCodeRecipe {
    private LengthUnit unit = LengthUnit.INCH;
    private double toolWidth = 0.25;
    private double depth = 0.1;
    private double stockSurface = 0;
    private double safetyHeight = 0.5;
    private double stepDown = 0.1;
    private double stepOver = 40;
    private int feedRate = 30;
    private int plungeRate = 30;
    private Direction direction = Direction.CLOCKWISE;

    public GCodePocketRecipe(int id) {
        super(id, GCodeRecipeType.POCKET);
        setName(String.format("pocket%d", id));
    }

    @Override
    public GCodePocketRecipe clone() {
        return (GCodePocketRecipe)super.clone();
    }

    @Override
    public void generateGCode(Shape<?> shape, GCodeBuilder builder) {
        if (shape instanceof Rectangle) {
            generateRectangleGCode((Rectangle)shape, builder);
        } else if (shape instanceof Circle) {
            generateCircleGCode((Circle)shape, builder);
        } else {
            throw new UnsupportedOperationException(String.format(
                    "Cannot generate %s type GCode for %s shape.",
                    getType(), shape.getClass().getSimpleName()));
        }
    }

    public void generateRectangleGCode(Rectangle shape, GCodeBuilder builder) {

    }

    public void generateCircleGCode(Circle shape, GCodeBuilder builder) {

    }
}
