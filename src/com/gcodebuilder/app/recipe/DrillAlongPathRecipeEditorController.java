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
import com.gcodebuilder.model.HoleAlignment;
import com.gcodebuilder.model.HoleSpacingMode;
import com.gcodebuilder.model.LengthUnit;
import com.gcodebuilder.recipe.GCodeDrillAlongPathRecipe;
import com.gcodebuilder.recipe.GCodeRecipeType;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;

public class DrillAlongPathRecipeEditorController extends AbstractRecipeTypeController<GCodeDrillAlongPathRecipe> {
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

    @FXML
    private ChoiceBox<Direction> directionCtl;

    @FXML
    private ChoiceBox<HoleSpacingMode> holeSpacingModeCtl;

    @FXML
    private TextField holeSpacingCtl;

    @FXML
    private ChoiceBox<HoleAlignment> holeAlignmentCtl;

    @FXML
    private TextField holeOffsetCtl;

    public DrillAlongPathRecipeEditorController() {
        super(GCodeRecipeType.DRILL_ALONG_PATH, GCodeDrillAlongPathRecipe.class);
    }

    @FXML
    public void initialize() {
        configuredChoiceBox(unitCtl, GCodeDrillAlongPathRecipe::getUnit,
                unitSetter, LengthUnit.values());
        configureTextField(toolWidthCtl, doubleFormatter(),
                GCodeDrillAlongPathRecipe::getToolWidth, GCodeDrillAlongPathRecipe::setToolWidth);
        configureTextField(depthCtl, doubleFormatter(),
                GCodeDrillAlongPathRecipe::getDepth, GCodeDrillAlongPathRecipe::setDepth);
        configureTextField(stockSurfaceCtl, doubleFormatter(),
                GCodeDrillAlongPathRecipe::getStockSurface, GCodeDrillAlongPathRecipe::setStockSurface);
        configureTextField(safetyHeightCtl, doubleFormatter(),
                GCodeDrillAlongPathRecipe::getSafetyHeight, GCodeDrillAlongPathRecipe::setSafetyHeight);
        configureTextField(stepDownCtl, doubleFormatter(),
                GCodeDrillAlongPathRecipe::getStepDown, GCodeDrillAlongPathRecipe::setStepDown);
        configureTextField(plungeRateCtl, integerFormatter(),
                GCodeDrillAlongPathRecipe::getPlungeRate, GCodeDrillAlongPathRecipe::setPlungeRate);
        configuredChoiceBox(directionCtl, GCodeDrillAlongPathRecipe::getDirection,
                GCodeDrillAlongPathRecipe::setDirection, Direction.values());
        configuredChoiceBox(holeSpacingModeCtl, GCodeDrillAlongPathRecipe::getHoleSpacingMode,
                GCodeDrillAlongPathRecipe::setHoleSpacingMode, HoleSpacingMode.values());
        configureTextField(holeSpacingCtl, doubleFormatter(),
                GCodeDrillAlongPathRecipe::getHoleSpacing, GCodeDrillAlongPathRecipe::setHoleSpacing);
        configuredChoiceBox(holeAlignmentCtl, GCodeDrillAlongPathRecipe::getHoleAlignment,
                GCodeDrillAlongPathRecipe::setHoleAlignment, HoleAlignment.values());
        configureTextField(holeOffsetCtl, doubleFormatter(),
                GCodeDrillAlongPathRecipe::getHoleOffset, GCodeDrillAlongPathRecipe::setHoleOffset);
    }

}
