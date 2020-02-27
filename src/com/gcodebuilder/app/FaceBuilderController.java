package com.gcodebuilder.app;

import com.gcodebuilder.generator.FacingGenerator;
import com.gcodebuilder.model.GCodeBuilder;
import com.gcodebuilder.model.GCodeProgram;
import com.gcodebuilder.model.LengthUnit;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;

public class FaceBuilderController {
    @FXML private GridPane rootPane;
    @FXML private ComboBox<LengthUnit> unitSelector;
    @FXML private TextField toolWidthInput;
    @FXML private TextField xInput;
    @FXML private TextField yInput;
    @FXML private TextField widthInput;
    @FXML private TextField lengthInput;
    @FXML private TextField depthInput;
    @FXML private TextField stockSurfaceInput;
    @FXML private TextField safetyHeightInput;
    @FXML private TextField stepDownInput;
    @FXML private TextField stepOverInput;
    @FXML private TextField borderWidthInput;
    @FXML private TextField feedRateInput;
    @FXML private TextField plungeRateInput;

    private static final DoubleStringConverter DOUBLE_CONVERTER = new DoubleStringConverter();
    private static TextFormatter<Double> doubleFormatter(double defaultValue) {
        return new TextFormatter<>(DOUBLE_CONVERTER, defaultValue);
    }

    private static final IntegerStringConverter INTEGER_CONVERTER = new IntegerStringConverter();
    private static TextFormatter<Integer> integerFormatter(int defaultValue) {
        return new TextFormatter<>(INTEGER_CONVERTER, defaultValue);
    }

    private final TextFormatter<Double> toolWidthFormatter = doubleFormatter(0.5);
    private final TextFormatter<Double> xFormatter = doubleFormatter(0);
    private final TextFormatter<Double> yFormatter = doubleFormatter(0);
    private final TextFormatter<Double> widthFormatter = doubleFormatter(1);
    private final TextFormatter<Double> lengthFormatter = doubleFormatter(1);
    private final TextFormatter<Double> depthFormatter = doubleFormatter(0.1);
    private final TextFormatter<Double> stockSurfaceFormatter = doubleFormatter(0);
    private final TextFormatter<Double> safetyHeightFormatter = doubleFormatter(0.5);
    private final TextFormatter<Double> stepDownFormatter = doubleFormatter(0.1);
    private final TextFormatter<Double> stepOverFormatter = doubleFormatter(40);
    private final TextFormatter<Double> borderWidthFormatter = doubleFormatter(30);
    private final TextFormatter<Integer> feedRateFormatter = integerFormatter(30);
    private final TextFormatter<Integer> plungeRateFormatter = integerFormatter(30);

    @FXML
    private void initialize() {
        unitSelector.getItems().addAll(LengthUnit.values());
        unitSelector.getSelectionModel().select(0);
        toolWidthInput.setTextFormatter(toolWidthFormatter);
        xInput.setTextFormatter(xFormatter);
        yInput.setTextFormatter(yFormatter);
        widthInput.setTextFormatter(widthFormatter);
        lengthInput.setTextFormatter(lengthFormatter);
        depthInput.setTextFormatter(depthFormatter);
        stockSurfaceInput.setTextFormatter(stockSurfaceFormatter);
        safetyHeightInput.setTextFormatter(safetyHeightFormatter);
        stepDownInput.setTextFormatter(stepDownFormatter);
        stepOverInput.setTextFormatter(stepOverFormatter);
        borderWidthInput.setTextFormatter(borderWidthFormatter);
        feedRateInput.setTextFormatter(feedRateFormatter);
        plungeRateInput.setTextFormatter(plungeRateFormatter);
    }

    @FXML
    private void generateGCode() {
        try {
            FacingGenerator generator = new FacingGenerator();
            generator.setUnit(unitSelector.getValue());
            generator.setX(xFormatter.getValue());
            generator.setY(yFormatter.getValue());
            generator.setWidth(widthFormatter.getValue());
            generator.setLength(lengthFormatter.getValue());
            generator.setDepth(depthFormatter.getValue());
            generator.setStockSurface(stockSurfaceFormatter.getValue());
            generator.setSafetyHeight(safetyHeightFormatter.getValue());
            generator.setStepDown(stepDownFormatter.getValue());
            generator.setStepOver(stepOverFormatter.getValue());
            generator.setBorderWidth(borderWidthFormatter.getValue());
            generator.setFeedRate(feedRateFormatter.getValue());
            generator.setPlungeRate(plungeRateFormatter.getValue());

            GCodeBuilder builder = new GCodeBuilder();
            generator.generateGCode(builder);
            GCodeProgram program = builder.build();

            FileChooser fileChooser = new FileChooser();
            FileChooser.ExtensionFilter gcodeExtFilter =
                    new FileChooser.ExtensionFilter("GCode", "*.nc");
            fileChooser.getExtensionFilters().add(gcodeExtFilter);
            fileChooser.setSelectedExtensionFilter(gcodeExtFilter);
            fileChooser.setInitialDirectory(new File("/home/zampire/Documents"));
            fileChooser.setInitialFileName("facing.nc");
            fileChooser.setTitle("Save GCode Program");
            File saveFile = fileChooser.showSaveDialog(rootPane.getScene().getWindow());

            if (saveFile != null) {
                PrintStream out = new PrintStream(saveFile);
                program.print(out);
                out.close();
            }
        } catch (Exception ex) {
            // TODO make error message box
            ex.printStackTrace();
        }
    }

}
