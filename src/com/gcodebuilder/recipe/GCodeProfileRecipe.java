package com.gcodebuilder.recipe;

import com.gcodebuilder.generator.GCodeGenerator;
import com.gcodebuilder.generator.GCodePathProfileGenerator;
import com.gcodebuilder.geometry.PathConvertible;
import com.gcodebuilder.geometry.PathGroup;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.model.Direction;
import com.gcodebuilder.model.LengthUnit;
import com.gcodebuilder.model.LengthUnitConverter;
import com.gcodebuilder.model.Side;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Getter
@Setter
public class GCodeProfileRecipe extends GCodeRecipe {
    private static final Logger log = LogManager.getLogger(GCodeProfileRecipe.class);

    private LengthUnit unit = LengthUnit.INCH;
    private double toolWidth = 0.25;
    private double depth = 0.1;
    private double stockSurface = 0;
    private double safetyHeight = 0.5;
    private double stepDown = 0.1;
    private int feedRate = 30;
    private int plungeRate = 30;
    private Side side = Side.OUTSIDE;
    private Direction direction = Direction.CLOCKWISE;

    public GCodeProfileRecipe(int id) {
        super(id, GCodeRecipeType.PROFILE);
        setName(String.format("profile%d", id));
    }

    @Override
    public GCodeProfileRecipe clone() {
        return (GCodeProfileRecipe)super.clone();
    }

    @Override
    public void convertToUnit(LengthUnit toUnit) {
        LengthUnitConverter converter = unit.getConverterTo(toUnit);
        setUnit(toUnit);
        setToolWidth(converter.convert(toolWidth));
        setDepth(converter.convert(depth));
        setStockSurface(converter.convert(stockSurface));
        setSafetyHeight(converter.convert(safetyHeight));
        setStepDown(converter.convert(stepDown));
        setFeedRate((int)Math.round(converter.convert(feedRate)));
        setPlungeRate((int)Math.round(converter.convert(plungeRate)));
    }

    @Override
    public GCodeGenerator getGCodeGenerator(Shape<?> shape) {
        if (shape instanceof PathConvertible) {
            return new GCodePathProfileGenerator(this, ((PathConvertible)shape).convertToPath());
        } else if (shape instanceof PathGroup) {
            return new GCodePathProfileGenerator(this, ((PathGroup)shape).getPaths());
        } else {
            return null;
        }
    }
}
