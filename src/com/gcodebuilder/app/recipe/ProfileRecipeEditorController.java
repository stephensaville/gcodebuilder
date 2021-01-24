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
                unitSetter, LengthUnit.values());
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
