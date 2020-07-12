package com.gcodebuilder.app;

import com.gcodebuilder.app.recipe.RecipeEditorController;
import com.gcodebuilder.app.tools.CircleTool;
import com.gcodebuilder.app.tools.EditTool;
import com.gcodebuilder.app.tools.EraseTool;
import com.gcodebuilder.app.tools.InteractionEvent;
import com.gcodebuilder.app.tools.MoveTool;
import com.gcodebuilder.app.tools.RectangleTool;
import com.gcodebuilder.app.tools.SelectionTool;
import com.gcodebuilder.app.tools.Tool;
import com.gcodebuilder.canvas.GCodeCanvas;
import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.model.UnitMode;
import com.gcodebuilder.recipe.GCodeRecipe;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class DrawingWindowController {
    private static final Logger log = LogManager.getLogger(DrawingWindowController.class);

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
    private Spinner<Double> majorGridCtl;

    @FXML
    private Spinner<Integer> minorGridCtl;

    @FXML
    private ScrollBar hScrollBar;

    @FXML
    private ScrollBar vScrollBar;

    @FXML
    private AnchorPane recipeEditorPane;

    private RectangleTool rectangleTool = new RectangleTool();
    private CircleTool circleTool = new CircleTool();
    private EditTool editTool = new EditTool();
    private MoveTool moveTool = new MoveTool();
    private EraseTool eraseTool = new EraseTool();
    private SelectionTool selectionTool = new SelectionTool();
    private Tool currentTool = rectangleTool;

    private Drawing drawing = new Drawing();

    private Point2D startPoint = Point2D.ZERO;
    private Point2D mouseStartPoint = Point2D.ZERO;
    private Shape currentShape;
    private Object currentHandle;

    private Set<Shape> currentSelectedShapes = Collections.emptySet();

    private FileOperations fileOperations;

    private RecipeEditorController recipeEditorController;

    @FXML
    public void initialize() throws IOException {
        unitCtl.getItems().addAll(UnitMode.values());
        unitCtl.setValue(UnitMode.INCH);
        unitCtl.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != canvas.getSettings().getUnits()) {
                canvas.getSettings().setUnits(newValue);
                canvas.refresh();
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
                canvas.refresh();
            }
        });

        SpinnerValueFactory<Double> gridSpacingValueFactory =
                new ExponentialSpinnerValueFactory(MIN_GRID_SPACING, MAX_GRID_SPACING,
                        canvas.getSettings().getMajorGridSpacing());
        gridSpacingValueFactory.setConverter(new DoubleStringConverterWithPrecision(4));
        majorGridCtl.setValueFactory(gridSpacingValueFactory);
        majorGridCtl.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != canvas.getSettings().getMajorGridSpacing()) {
                canvas.getSettings().setMajorGridSpacing(newValue);
                canvas.refresh();
            }
        });

        SpinnerValueFactory<Integer> minorGridValueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1, 100, canvas.getSettings().getMinorGridDivision(), 1);
        minorGridCtl.setValueFactory(minorGridValueFactory);
        minorGridCtl.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != canvas.getSettings().getMinorGridDivision()) {
                canvas.getSettings().setMinorGridDivision(newValue);
                canvas.refresh();
            }
        });

        hScrollBar.setUnitIncrement(1);
        hScrollBar.setBlockIncrement(10);
        hScrollBar.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.doubleValue() != canvas.getOriginX()) {
                canvas.setOriginX(newValue.doubleValue());
                canvas.refresh();
            }
        });

        vScrollBar.setUnitIncrement(1);
        vScrollBar.setBlockIncrement(10);
        vScrollBar.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.doubleValue() != canvas.getOriginY()) {
                canvas.setOriginY(newValue.doubleValue());
                canvas.refresh();
            }
        });

        canvas.getDrawables().add(drawing);

        fileOperations = new FileOperations(rootPane);

        recipeEditorController = RecipeEditorController.attach(recipeEditorPane);

        recipeEditorController.currentRecipeProperty().addListener((obs, oldRecipe, newRecipe) -> {
            log.info(String.format("Current recipe changed: %s -> %s", oldRecipe, newRecipe));
            int newRecipeId = 0;
            if (newRecipe != null) {
                drawing.putRecipe(newRecipe);
                newRecipeId = newRecipe.getId();
            }
            for (Shape shape : currentSelectedShapes) {
                shape.setRecipeId(newRecipeId);
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

    private void setCurrentTool(Tool tool) {
        currentTool = tool;
    }

    public void selectRectangleTool() {
        setCurrentTool(rectangleTool);
    }

    public void selectCircleTool() {
        setCurrentTool(circleTool);
    }

    public void selectEditTool() {
        setCurrentTool(editTool);
    }

    public void selectMoveTool() {
        setCurrentTool(moveTool);
    }

    public void selectEraseTool() {
        setCurrentTool(eraseTool);
    }

    public void selectSelectionTool() {
        setCurrentTool(selectionTool);
    }

    private InteractionEvent makeToolEvent(MouseEvent event, boolean restart) {
        Point2D point = canvas.mouseToGrid(event, true);
        Point2D mousePoint = canvas.mouseToGrid(event, false);
        if (restart) {
            startPoint = point;
            mouseStartPoint = mousePoint;
            currentShape = null;
            currentHandle = null;
            for (Shape shape : drawing.getShapes()) {
                currentHandle = shape.getHandle(point, mousePoint, canvas.getPixelsPerUnit());
                if (currentHandle != null) {
                    currentShape = shape;
                    break;
                }
            }
        }
        return new InteractionEvent(
                drawing, event,
                point, startPoint,
                mousePoint, mouseStartPoint,
                currentShape, currentHandle);
    }

    private void checkSelectedShapes() {
        // switch to recipe attached to selected shape
        Set<Shape> selectedShapes = drawing.getSelectedShapes();
        if (currentSelectedShapes.equals(selectedShapes)) {
            return;
        }
        log.info(String.format("Selection changed: %s -> %s", currentSelectedShapes, selectedShapes));
        currentSelectedShapes = Collections.emptySet();
        if (selectedShapes.isEmpty()) {
            recipeEditorController.clearCurrentRecipe();
        } else {
            Set<Integer> selectedShapeRecipes =
                    selectedShapes.stream()
                            .map(Shape::getRecipeId)
                            .collect(Collectors.toSet());
            log.info(String.format("Selected shape recipes found: %s", selectedShapeRecipes));
            if (selectedShapeRecipes.size() == 1) {
                int currentRecipeId = selectedShapeRecipes.iterator().next();
                if (currentRecipeId > 0) {
                    GCodeRecipe currentRecipe = drawing.getRecipe(currentRecipeId);
                    recipeEditorController.setCurrentRecipe(currentRecipe);
                } else {
                    recipeEditorController.clearCurrentRecipe();
                }
            } else {
                recipeEditorController.clearCurrentRecipe();
            }
        }
        currentSelectedShapes = selectedShapes;
        drawing.setDirty(true);
    }

    private void refreshDrawingWhenDirty() {
        if (drawing.isDirty()) {
            canvas.refresh();
        }
    }

    public void mousePressOnCanvas(MouseEvent event) {
        InteractionEvent toolEvent = makeToolEvent(event, true);
        if (currentTool != null) {
            currentShape = currentTool.down(toolEvent);
            if (currentTool.isSelectionTool()) {
                checkSelectedShapes();
            } else {
                // default behavior select current shape for non-selection tools
                boolean selectionChanged = false;
                if (drawing.unselectAllShapes() > 0) {
                    selectionChanged = true;
                }
                if (currentShape != null) {
                    currentShape.setSelected(true);
                    selectionChanged = true;
                }
                if (selectionChanged) {
                    checkSelectedShapes();
                }
            }
        }
        refreshDrawingWhenDirty();
    }

    public void mouseDragOnCanvas(MouseEvent event) {
        InteractionEvent toolEvent = makeToolEvent(event, false);
        if (currentTool != null) {
            currentTool.drag(toolEvent);
            if (currentTool.isSelectionTool()) {
                checkSelectedShapes();
            }
        }
        refreshDrawingWhenDirty();
    }

    public void mouseReleaseOnCanvas(MouseEvent event) {
        InteractionEvent toolEvent = makeToolEvent(event, false);
        if (currentTool != null) {
            currentTool.up(toolEvent);
            if (currentTool.isSelectionTool()) {
                checkSelectedShapes();
            }
        }
        refreshDrawingWhenDirty();
    }

    public void openDrawing() {
        Drawing newDrawing = fileOperations.open();
        if (newDrawing != null) {
            canvas.getDrawables().remove(drawing);
            drawing = newDrawing;
            canvas.getDrawables().add(newDrawing);

            recipeEditorController.clearCurrentRecipe();
            recipeEditorController.getRecipes().clear();
            recipeEditorController.getRecipes().addAll(newDrawing.getRecipes());

            canvas.refresh();
        }
    }

    public void saveDrawing() {
        fileOperations.save(drawing);
    }

    public void closeWindow() {
        Stage stage = (Stage)rootPane.getScene().getWindow();
        stage.close();
    }
}
