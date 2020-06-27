package com.gcodebuilder.app;

import com.gcodebuilder.model.Direction;
import com.gcodebuilder.model.LengthUnit;
import com.gcodebuilder.model.Side;
import com.gcodebuilder.recipe.GCodeProfileRecipe;
import com.gcodebuilder.recipe.GCodeRecipeType;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;

public class ProfileRecipeEditorController extends AbstractRecipeTypeController<GCodeProfileRecipe> {
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
    private ChoiceBox<Side> sideCtl;

    @FXML
    private ChoiceBox<Direction> directionCtl;

    public ProfileRecipeEditorController() {
        super(GCodeRecipeType.PROFILE, GCodeProfileRecipe.class);
    }

    @FXML
    public void initialize() {
        configuredChoiceBox(unitCtl, GCodeProfileRecipe::getUnit,
                GCodeProfileRecipe::setUnit, LengthUnit.values());
        configureTextField(toolWidthCtl, doubleFormatter(),
                GCodeProfileRecipe::getToolWidth, GCodeProfileRecipe::setToolWidth);
        configureTextField(depthCtl, doubleFormatter(),
                GCodeProfileRecipe::getDepth, GCodeProfileRecipe::setDepth);
        configureTextField(stockSurfaceCtl, doubleFormatter(),
                GCodeProfileRecipe::getStockSurface, GCodeProfileRecipe::setStockSurface);
        configureTextField(safetyHeightCtl, doubleFormatter(),
                GCodeProfileRecipe::getSafetyHeight, GCodeProfileRecipe::setSafetyHeight);
        configureTextField(stepDownCtl, doubleFormatter(),
                GCodeProfileRecipe::getStepDown, GCodeProfileRecipe::setStepDown);
        configureTextField(stepOverCtl, doubleFormatter(),
                GCodeProfileRecipe::getStepOver, GCodeProfileRecipe::setStepOver);
        configureTextField(feedRateCtl, integerFormatter(),
                GCodeProfileRecipe::getFeedRate, GCodeProfileRecipe::setFeedRate);
        configureTextField(plungeRateCtl, integerFormatter(),
                GCodeProfileRecipe::getPlungeRate, GCodeProfileRecipe::setPlungeRate);
        configuredChoiceBox(sideCtl, GCodeProfileRecipe::getSide,
                GCodeProfileRecipe::setSide, Side.values());
        configuredChoiceBox(directionCtl, GCodeProfileRecipe::getDirection,
                GCodeProfileRecipe::setDirection, Direction.values());
    }

}
