package com.gcodebuilder.app.recipe;

import com.gcodebuilder.model.Direction;
import com.gcodebuilder.model.LengthUnit;
import com.gcodebuilder.recipe.GCodePocketRecipe;
import com.gcodebuilder.recipe.GCodeRecipeType;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;

public class PocketRecipeEditorController extends AbstractRecipeTypeController<GCodePocketRecipe> {
    @FXML
    private ChoiceBox<LengthUnit> unitCtl;

    @FXML
    private TextField toolWidthCtl;

    @FXML
    private TextField depthCtl;

    @FXML
    private TextField stockSurfaceCtl;

    @FXML
    private TextField safetyHeightCtl;

    @FXML
    private TextField stepDownCtl;

    @FXML
    private TextField stepOverCtl;

    @FXML
    private TextField feedRateCtl;

    @FXML
    private TextField plungeRateCtl;

    @FXML
    private ChoiceBox<Direction> directionCtl;

    public PocketRecipeEditorController() {
        super(GCodeRecipeType.POCKET, GCodePocketRecipe.class);
    }

    @FXML
    public void initialize() {
        configuredChoiceBox(unitCtl, GCodePocketRecipe::getUnit,
                GCodePocketRecipe::setUnit, LengthUnit.values());
        configureTextField(toolWidthCtl, doubleFormatter(),
                GCodePocketRecipe::getToolWidth, GCodePocketRecipe::setToolWidth);
        configureTextField(depthCtl, doubleFormatter(),
                GCodePocketRecipe::getDepth, GCodePocketRecipe::setDepth);
        configureTextField(stockSurfaceCtl, doubleFormatter(),
                GCodePocketRecipe::getStockSurface, GCodePocketRecipe::setStockSurface);
        configureTextField(safetyHeightCtl, doubleFormatter(),
                GCodePocketRecipe::getSafetyHeight, GCodePocketRecipe::setSafetyHeight);
        configureTextField(stepDownCtl, doubleFormatter(),
                GCodePocketRecipe::getStepDown, GCodePocketRecipe::setStepDown);
        configureTextField(stepOverCtl, doubleFormatter(),
                GCodePocketRecipe::getStepOver, GCodePocketRecipe::setStepOver);
        configureTextField(feedRateCtl, integerFormatter(),
                GCodePocketRecipe::getFeedRate, GCodePocketRecipe::setFeedRate);
        configureTextField(plungeRateCtl, integerFormatter(),
                GCodePocketRecipe::getPlungeRate, GCodePocketRecipe::setPlungeRate);
        configuredChoiceBox(directionCtl, GCodePocketRecipe::getDirection,
                GCodePocketRecipe::setDirection, Direction.values());
    }

}
