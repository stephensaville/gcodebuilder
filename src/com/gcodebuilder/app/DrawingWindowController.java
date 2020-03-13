package com.gcodebuilder.app;

import com.gcodebuilder.canvas.GCodeCanvas;
import com.gcodebuilder.model.UnitMode;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;

public class DrawingWindowController {

    private static final double MIN_ZOOM = 1;
    private static final double MAX_ZOOM = 6400;
    private static final double MAX_GRID_WIDTH_INCHES = 32;
    private static final double MIN_GRID_SPACING = 0.01;
    private static final double MAX_GRID_SPACING = 10;
    private static final double GRID_SPACING_INCREMENT = 0.1;

    @FXML
    private BorderPane rootPane;

    @FXML
    private GCodeCanvas canvas;

    @FXML
    private ChoiceBox<UnitMode> unitCtl;

    @FXML
    private Spinner<Double> zoomCtl;

    @FXML
    private Spinner<Double> gridSpacingCtl;

    @FXML
    private ScrollBar hScrollBar;

    @FXML
    private ScrollBar vScrollBar;

    @FXML
    public void initialize() {
        unitCtl.getItems().addAll(UnitMode.values());
        unitCtl.setValue(UnitMode.INCH);
        unitCtl.valueProperty().addListener((obs, oldValue, newValue) -> {
            canvas.setUnitMode(newValue);
        });

        SpinnerValueFactory.DoubleSpinnerValueFactory zoomValueFactory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(
                        MIN_ZOOM, MAX_ZOOM, canvas.getZoom()*100.0, 100.0) {

                    @Override
                    public void decrement(int i) {
                        setValue(getValue() * Math.pow(2, -i));
                    }

                    @Override
                    public void increment(int i) {
                        setValue(getValue() * Math.pow(2, i));
                    }
                };
        zoomCtl.setValueFactory(zoomValueFactory);
        zoomCtl.valueProperty().addListener((obs, oldValue, newValue) -> {
            canvas.setZoom(newValue/100);
        });

        gridSpacingCtl.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(
                MIN_GRID_SPACING, MAX_GRID_SPACING, 1.0, GRID_SPACING_INCREMENT));
        gridSpacingCtl.valueProperty().addListener((obs, oldValue, newValue) -> {
            canvas.setGridSpacing(newValue);
        });

        double dpi = Screen.getPrimary().getDpi();
        double maxScreenWidth = MAX_GRID_WIDTH_INCHES * dpi;

        hScrollBar.setMin(-maxScreenWidth);
        hScrollBar.setMax(maxScreenWidth);
        hScrollBar.setUnitIncrement(1);
        hScrollBar.setBlockIncrement(10);
        hScrollBar.setValue(0);
        hScrollBar.valueProperty().addListener((obs, oldValue, newValue) -> {
            canvas.setOriginX(newValue.doubleValue());
        });

        vScrollBar.setMin(-maxScreenWidth);
        vScrollBar.setMax(maxScreenWidth);
        vScrollBar.setUnitIncrement(1);
        vScrollBar.setBlockIncrement(10);
        vScrollBar.valueProperty().addListener((obs, oldValue, newValue) -> {
            canvas.setOriginY(newValue.doubleValue());
        });
    }

    private double measureHeight(Node child) {
        if (child == null) {
            return 0;
        }
        double height = child.getBoundsInParent().getHeight();
        Insets margin = BorderPane.getMargin(child);
        if (margin != null) {
            height += margin.getTop() + margin.getBottom();
        }
        return height;
    }

    private double measureWidth(Node child) {
        if (child == null) {
            return 0;
        }
        double width = child.getBoundsInParent().getWidth();
        Insets margin = BorderPane.getMargin(child);
        if (margin != null) {
            width += margin.getLeft() + margin.getRight();
        }
        return width;
    }

    public void bindProperties() {
        DoubleBinding widthBinding = rootPane.widthProperty()
                .subtract(measureWidth(rootPane.getLeft()))
                .subtract(measureWidth(rootPane.getRight()))
                .subtract(measureWidth(vScrollBar));
        canvas.widthProperty().bind(widthBinding);

        DoubleBinding heightBinding = rootPane.heightProperty()
                .subtract(measureHeight(rootPane.getTop()))
                .subtract(measureHeight(rootPane.getBottom()))
                .subtract(measureHeight(hScrollBar));
        canvas.heightProperty().bind(heightBinding);

        hScrollBar.setValue(canvas.getWidth()/2);
        vScrollBar.setValue(canvas.getHeight()/2);
    }


}
