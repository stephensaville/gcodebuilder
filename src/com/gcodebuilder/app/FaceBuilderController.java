package com.gcodebuilder.app;

import com.gcodebuilder.model.LengthUnit;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class FaceBuilderController {
    @FXML private ComboBox<LengthUnit> unitSelector;
    @FXML private TextField toolWidthInput;
    @FXML private TextField xInput;
    @FXML private TextField yInput;
    @FXML private TextField widthInput;
    @FXML private TextField heightInput;
    @FXML private TextField depthInput;
    @FXML private TextField topOfMaterialInput;
    @FXML private TextField stepDownInput;
    @FXML private TextField stepOverInput;
    @FXML private TextField feedRateInput;
    @FXML private TextField plungeRateInput;

    @FXML
    private void initialize() {
        unitSelector.getItems().addAll(LengthUnit.values());
        unitSelector.getSelectionModel().select(0);
    }

    @FXML
    private void generateGCode() {

    }

}
