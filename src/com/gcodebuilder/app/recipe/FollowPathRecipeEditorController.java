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
import com.gcodebuilder.recipe.GCodeFollowPathRecipe;
import com.gcodebuilder.recipe.GCodePocketRecipe;
import com.gcodebuilder.recipe.GCodeRecipeType;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;

public class FollowPathRecipeEditorController extends AbstractRecipeTypeController<GCodeFollowPathRecipe> {
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
    private ChoiceBox<Direction> directionCtl;

    public FollowPathRecipeEditorController() {
        super(GCodeRecipeType.FOLLOW_PATH, GCodeFollowPathRecipe.class);
    }

    @FXML
    public void initialize() {
        configuredChoiceBox(unitCtl, GCodeFollowPathRecipe::getUnit,
                unitSetter, LengthUnit.values());
        configureTextField(toolWidthCtl, doubleFormatter(),
                GCodeFollowPathRecipe::getToolWidth, GCodeFollowPathRecipe::setToolWidth);
        configureTextField(depthCtl, doubleFormatter(),
                GCodeFollowPathRecipe::getDepth, GCodeFollowPathRecipe::setDepth);
        configureTextField(stockSurfaceCtl, doubleFormatter(),
                GCodeFollowPathRecipe::getStockSurface, GCodeFollowPathRecipe::setStockSurface);
        configureTextField(safetyHeightCtl, doubleFormatter(),
                GCodeFollowPathRecipe::getSafetyHeight, GCodeFollowPathRecipe::setSafetyHeight);
        configureTextField(stepDownCtl, doubleFormatter(),
                GCodeFollowPathRecipe::getStepDown, GCodeFollowPathRecipe::setStepDown);
        configureTextField(feedRateCtl, integerFormatter(),
                GCodeFollowPathRecipe::getFeedRate, GCodeFollowPathRecipe::setFeedRate);
        configureTextField(plungeRateCtl, integerFormatter(),
                GCodeFollowPathRecipe::getPlungeRate, GCodeFollowPathRecipe::setPlungeRate);
        configuredChoiceBox(directionCtl, GCodeFollowPathRecipe::getDirection,
                GCodeFollowPathRecipe::setDirection, Direction.values());
    }

}
