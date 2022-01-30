/*
 * Copyright (c) 2021 Stephen Saville
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gcodebuilder.app.recipe;

import com.gcodebuilder.model.LengthUnit;
import com.gcodebuilder.recipe.GCodeCenterDrillRecipe;
import com.gcodebuilder.recipe.GCodeRecipeType;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;

public class CenterDrillRecipeEditorController extends AbstractRecipeTypeController<GCodeCenterDrillRecipe> {
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
    private TextField plungeRateCtl;

    public CenterDrillRecipeEditorController() {
        super(GCodeRecipeType.CENTER_DRILL, GCodeCenterDrillRecipe.class);
    }

    @FXML
    public void initialize() {
        configuredChoiceBox(unitCtl, GCodeCenterDrillRecipe::getUnit,
                unitSetter, LengthUnit.values());
        configureTextField(toolWidthCtl, doubleFormatter(),
                GCodeCenterDrillRecipe::getToolWidth, GCodeCenterDrillRecipe::setToolWidth);
        configureTextField(depthCtl, doubleFormatter(),
                GCodeCenterDrillRecipe::getDepth, GCodeCenterDrillRecipe::setDepth);
        configureTextField(stockSurfaceCtl, doubleFormatter(),
                GCodeCenterDrillRecipe::getStockSurface, GCodeCenterDrillRecipe::setStockSurface);
        configureTextField(safetyHeightCtl, doubleFormatter(),
                GCodeCenterDrillRecipe::getSafetyHeight, GCodeCenterDrillRecipe::setSafetyHeight);
        configureTextField(stepDownCtl, doubleFormatter(),
                GCodeCenterDrillRecipe::getStepDown, GCodeCenterDrillRecipe::setStepDown);
        configureTextField(plungeRateCtl, integerFormatter(),
                GCodeCenterDrillRecipe::getPlungeRate, GCodeCenterDrillRecipe::setPlungeRate);
    }

}
