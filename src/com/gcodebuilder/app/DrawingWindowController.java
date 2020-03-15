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

public class DrawingWindowController {

    private static final double MIN_ZOOM = 1.5625;
    private static final double MAX_ZOOM = 6400;
    private static final double MIN_GRID_SPACING = 0.0625;
    private static final double MAX_GRID_SPACING = 32;

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
            if (newValue != canvas.getSettings().getUnits()) {
                canvas.getSettings().setUnits(newValue);
                canvas.refreshGrid();
            }
        });

        SpinnerValueFactory<Double> zoomValueFactory =
                new ExponentialSpinnerValueFactory(MIN_ZOOM, MAX_ZOOM, canvas.getZoom()*100);
        zoomValueFactory.setConverter(new DoubleStringConverterWithPrecision(4));
        zoomCtl.setValueFactory(zoomValueFactory);
        zoomCtl.valueProperty().addListener((obs, oldValue, newValue) -> {
            double newZoom = newValue/100;
            if (newZoom != canvas.getZoom()) {
                canvas.setZoom(newValue / 100);
                canvas.refreshGrid();
            }
        });

        SpinnerValueFactory<Double> gridSpacingValueFactory =
                new ExponentialSpinnerValueFactory(MIN_GRID_SPACING, MAX_GRID_SPACING,
                        canvas.getSettings().getMajorGridSpacing());
        gridSpacingValueFactory.setConverter(new DoubleStringConverterWithPrecision(4));
        gridSpacingCtl.setValueFactory(gridSpacingValueFactory);
        gridSpacingCtl.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != canvas.getSettings().getMajorGridSpacing()) {
                canvas.getSettings().setMajorGridSpacing(newValue);
                canvas.refreshGrid();
            }
        });

        hScrollBar.setUnitIncrement(1);
        hScrollBar.setBlockIncrement(10);
        hScrollBar.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.doubleValue() != canvas.getOriginX()) {
                canvas.setOriginX(newValue.doubleValue());
                canvas.refreshGrid();
            }
        });

        vScrollBar.setUnitIncrement(1);
        vScrollBar.setBlockIncrement(10);
        vScrollBar.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.doubleValue() != canvas.getOriginY()) {
                canvas.setOriginY(newValue.doubleValue());
                canvas.refreshGrid();
            }
        });
    }

    private void updateScrollBars(Rectangle2D originArea) {
        hScrollBar.setMin(originArea.getMinX());
        hScrollBar.setMax(originArea.getMaxX());
        hScrollBar.setValue(canvas.getOriginX());
        vScrollBar.setMin(originArea.getMinY());
        vScrollBar.setMax(originArea.getMaxY());
        vScrollBar.setValue(canvas.getOriginY());
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

        updateScrollBars(canvas.getOriginArea());

        hScrollBar.setValue(canvas.getWidth()/2);
        vScrollBar.setValue(canvas.getHeight()/2);

        canvas.originAreaProperty().addListener((obs, oldValue, newValue) -> {
            updateScrollBars(newValue);
        });
    }


}
