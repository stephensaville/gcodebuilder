package com.gcodebuilder.app;

import com.gcodebuilder.app.recipe.RecipeEditorController;
import com.gcodebuilder.app.shapes.ObservableShape;
import com.gcodebuilder.app.shapes.ShapesTableController;
import com.gcodebuilder.app.tools.CircleTool;
import com.gcodebuilder.app.tools.EditTool;
import com.gcodebuilder.app.tools.EraseTool;
import com.gcodebuilder.app.tools.InteractionEvent;
import com.gcodebuilder.app.tools.MoveTool;
import com.gcodebuilder.app.tools.PathTool;
import com.gcodebuilder.app.tools.RectangleTool;
import com.gcodebuilder.app.tools.ResizeTool;
import com.gcodebuilder.app.tools.SelectionTool;
import com.gcodebuilder.app.tools.Tool;
import com.gcodebuilder.canvas.GCodeCanvas;
import com.gcodebuilder.changelog.Change;
import com.gcodebuilder.changelog.ChangeLog;
import com.gcodebuilder.changelog.SelectionChange;
import com.gcodebuilder.changelog.ShapeListChange;
import com.gcodebuilder.changelog.Snapshot;
import com.gcodebuilder.generator.GCodeGenerator;
import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Path;
import com.gcodebuilder.geometry.PathGroup;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.model.ArcDistanceMode;
import com.gcodebuilder.model.DistanceMode;
import com.gcodebuilder.model.FeedRateMode;
import com.gcodebuilder.model.GCodeBuilder;
import com.gcodebuilder.model.GCodeProgram;
import com.gcodebuilder.model.LengthUnit;
import com.gcodebuilder.recipe.GCodeRecipe;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.ListChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DrawingWindowController {
    private static final Logger log = LogManager.getLogger(DrawingWindowController.class);

    private static final double MIN_ZOOM = 1.5625;
    private static final double MAX_ZOOM = 6400;
    private static final double MIN_GRID_SPACING = 0.0625;
    private static final double MAX_GRID_SPACING = 32;

    @FXML
    private ToggleGroup tools;

    @FXML
    private BorderPane rootPane;

    @FXML
    private GCodeCanvas canvas;

    @FXML
    private ChoiceBox<LengthUnit> unitCtl;

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
    private TitledPane shapesPane;

    @FXML
    private TitledPane recipeEditorPane;

    @FXML
    private TextArea gcodeEditor;

    @FXML
    private MenuItem saveGCodeItem;

    @FXML
    private MenuItem saveGCodeAsItem;

    @FXML
    private MenuItem undoItem;

    @FXML
    private MenuItem redoItem;

    @FXML
    private MenuItem cutItem;

    @FXML
    private MenuItem copyItem;

    @FXML
    private MenuItem pasteItem;

    @FXML
    private MenuItem deleteItem;

    @FXML
    private MenuItem groupPathsItem;

    @FXML
    private MenuItem ungroupPathsItem;

    private final RectangleTool rectangleTool = new RectangleTool();
    private final CircleTool circleTool = new CircleTool();
    private final PathTool pathTool = new PathTool();
    private final EditTool editTool = new EditTool();
    private final MoveTool moveTool = new MoveTool();
    private final EraseTool eraseTool = new EraseTool();
    private final SelectionTool selectionTool = new SelectionTool();
    private final ResizeTool resizeTool = new ResizeTool();
    private Tool currentTool = selectionTool;

    private Drawing drawing = new Drawing();

    private Point2D startPoint = Point2D.ZERO;
    private Point2D mouseStartPoint = Point2D.ZERO;
    private Shape<?> currentShape;
    private Object currentHandle;

    private Set<Shape<?>> currentSelectedShapes = Collections.emptySet();

    private final ChangeLog changeLog = new ChangeLog();
    private Supplier<Change> changeSupplier = null;

    private static final String DEFAULT_DRAWING_FILENAME = "drawing.json";
    private static final String DEFAULT_GCODE_FILENAME = "toolpath.nc";
    private static final String DEFAULT_IMAGE_FILENAME = "drawing.png";

    private static final FileChooser.ExtensionFilter[] DRAWING_FILE_EXTENSIONS = {
            new FileChooser.ExtensionFilter("JSON", "*.json")
    };
    private static final FileChooser.ExtensionFilter[] GCODE_FILE_EXTENSIONS = {
            new FileChooser.ExtensionFilter("GCode", "*.nc")
    };
    private static final FileChooser.ExtensionFilter[] IMAGE_FILE_EXTENSIONS = {
            new FileChooser.ExtensionFilter("PNG", "*.png")
    };

    private FileOperations<Drawing> drawingFileOperations;
    private FileOperations<GCodeProgram> gCodeFileOperations;
    private FileOperations<Image> imageFileOperations;

    private ShapesTableController shapesTableController;
    private RecipeEditorController recipeEditorController;

    private GCodeProgram gCodeProgram;

    @FXML
    public void initialize() throws IOException {
        unitCtl.getItems().addAll(LengthUnit.values());
        unitCtl.setValue(LengthUnit.INCH);
        unitCtl.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != drawing.getLengthUnit()) {
                drawing.setLengthUnit(newValue);
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
                canvas.setOriginX(canvas.getOriginArea().getMaxX() - newValue.doubleValue());
                canvas.refresh();
            }
        });

        vScrollBar.setUnitIncrement(1);
        vScrollBar.setBlockIncrement(10);
        vScrollBar.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.doubleValue() != canvas.getOriginY()) {
                canvas.setOriginY(canvas.getOriginArea().getMaxY() - newValue.doubleValue());
                canvas.refresh();
            }
        });

        canvas.getDrawables().add(drawing);

        drawingFileOperations = new FileOperations<>(
                rootPane, Drawing::load, Drawing::save,
                "Drawing", DEFAULT_DRAWING_FILENAME,
                DRAWING_FILE_EXTENSIONS);

        gCodeFileOperations = new FileOperations<>(
                rootPane, GCodeProgram::load, GCodeProgram::save,
                "GCode", DEFAULT_GCODE_FILENAME,
                GCODE_FILE_EXTENSIONS);

        imageFileOperations = new FileOperations<>(
                rootPane, Image::new, this::saveImageToFile,
                "Image", DEFAULT_IMAGE_FILENAME,
                IMAGE_FILE_EXTENSIONS);

        shapesTableController = ShapesTableController.attach(shapesPane);

        shapesPane.expandedProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                shapesTableController.syncShapes(drawing);
            }
        });

        shapesTableController.getSelectedShapes()
                .getSelectedItems().addListener((ListChangeListener<ObservableShape>) change -> {
            boolean selectedChanged = false;
            while (change.next()) {
                if (change.wasAdded()) {
                    for (ObservableShape obsShape : change.getAddedSubList()) {
                        if (!obsShape.getShape().isSelected()) {
                            obsShape.getShape().setSelected(true);
                            selectedChanged = true;
                        }
                    }
                }
                if (change.wasRemoved()) {
                    for (ObservableShape obsShape : change.getRemoved()) {
                        if (obsShape.getShape().isSelected()) {
                            obsShape.getShape().setSelected(false);
                            selectedChanged = true;
                        }
                    }
                }
            }
            if (selectedChanged) {
                drawing.setDirty(true);
                canvas.refresh();
                checkSelectedShapes();
            }
        });

        recipeEditorController = RecipeEditorController.attach(recipeEditorPane);

        recipeEditorController.currentRecipeProperty().addListener((obs, oldRecipe, newRecipe) -> {
            log.info(String.format("Current recipe changed: %s -> %s", oldRecipe, newRecipe));
            int newRecipeId = 0;
            if (newRecipe != null) {
                drawing.putRecipe(newRecipe);
                newRecipeId = newRecipe.getId();
            }
            for (Shape<?> shape : currentSelectedShapes) {
                shape.setRecipeId(newRecipeId);
            }
        });

        recipeEditorController.getRecipes().addListener((ListChangeListener<GCodeRecipe>) change -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    for (GCodeRecipe removed : change.getRemoved()) {
                        drawing.removeRecipe(removed);
                    }
                }
            }
        });

        updateClipboardMenuItems();
    }

    private void updateScrollBars(Rectangle2D originArea) {
        log.info("updateScrollBars originArea={}", originArea);
        hScrollBar.setMin(0);
        hScrollBar.setMax(originArea.getWidth());
        hScrollBar.setValue(originArea.getMaxX() - canvas.getOriginX());
        vScrollBar.setMin(0);
        vScrollBar.setMax(originArea.getHeight());
        vScrollBar.setValue(originArea.getMaxY() - canvas.getOriginY());
    }

    public void bindProperties() {
        DoubleBinding widthBinding = rootPane.widthProperty()
                .subtract(NodeSize.measureWidth(rootPane.getLeft()))
                .subtract(NodeSize.measureWidth(rootPane.getRight()))
                .subtract(NodeSize.measureWidth(vScrollBar));
        canvas.widthProperty().bind(widthBinding);

        DoubleBinding heightBinding = rootPane.heightProperty()
                .subtract(NodeSize.measureHeight(rootPane.getTop()))
                .subtract(NodeSize.measureHeight(rootPane.getBottom()))
                .subtract(NodeSize.measureHeight(hScrollBar));
        canvas.heightProperty().bind(heightBinding);

        updateScrollBars(canvas.getOriginArea());

        GridSettings settings = canvas.getSettings();
        double minorGridWidth = canvas.getPixelsPerUnit() * settings.getMajorGridSpacing() / settings.getMinorGridDivision();
        double xOriginOffset = (canvas.getWidth() % minorGridWidth) / 2;
        double yOriginOffset = (canvas.getHeight() % minorGridWidth) / 2;
        log.info("minorGridWidth={}", minorGridWidth);

        hScrollBar.setValue(canvas.getOriginArea().getMaxX() - xOriginOffset);
        vScrollBar.setValue(canvas.getOriginArea().getMaxY() - canvas.getHeight() + yOriginOffset);

        canvas.heightProperty().addListener((obs, oldValue, newValue) -> {
            double heightChange = newValue.doubleValue() - oldValue.doubleValue();
            vScrollBar.setValue(canvas.getOriginArea().getMaxX() - canvas.getOriginY() - heightChange);
        });

        canvas.originAreaProperty().addListener((obs, oldValue, newValue) -> {
            log.info("originAreaProperty updated oldValue={} newValue={}", oldValue, newValue);
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

    public void selectPathTool() {
        setCurrentTool(pathTool);
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

    public void selectResizeTool() {
        setCurrentTool(resizeTool);
    }

    private void updateChangeLogMenuItems() {
        if (changeLog.isUndoEnabled()) {
            undoItem.setText(String.format("_Undo %s", changeLog.getUndoDescription()));
            undoItem.setDisable(false);
        } else {
            undoItem.setText("_Undo");
            undoItem.setDisable(true);
        }
        if (changeLog.isRedoEnabled()) {
            redoItem.setText(String.format("_Redo %s", changeLog.getRedoDescription()));
            redoItem.setDisable(false);
        } else {
            redoItem.setText("_Redo");
            redoItem.setDisable(true);
        }
    }

    public void doChange(Change change) {
        if (change != null) {
            changeLog.doChange(change);
            updateChangeLogMenuItems();
        }
    }

    public void undoChange() {
        changeLog.undoChange();
        drawing.setDirty(true);
        updateChangeLogMenuItems();
        checkForChanges();
    }

    public void redoChange() {
        changeLog.redoChange();
        drawing.setDirty(true);
        updateChangeLogMenuItems();
        checkForChanges();
    }

    private void updateGroupPathsMenuItems() {
        int pathsSelected = 0;
        int pathGroupsSelected = 0;
        for (Shape<?> shape : currentSelectedShapes) {
            if (shape instanceof Path) {
                ++pathsSelected;
            } else if (shape instanceof PathGroup) {
                ++pathGroupsSelected;
            }
        }
        groupPathsItem.setDisable(pathsSelected + pathGroupsSelected < 2);
        ungroupPathsItem.setDisable(pathGroupsSelected < 1);
    }

    public void groupPaths() {
        Snapshot<List<Shape<?>>> shapesBefore = drawing.saveShapes();
        PathGroup group = PathGroup.groupSelected(drawing);
        if (group.isVisible()) {
            group.setSelected(true);
            doChange(new ShapeListChange("Group Paths", shapesBefore, drawing.saveShapes()));
            drawing.setDirty(true);
        }
        checkForChanges();
    }

    public void ungroupPaths() {
        Snapshot<List<Shape<?>>> shapesBefore = drawing.saveShapes();
        boolean shapesChanged = false;
        for (PathGroup pathGroup : drawing.getSelectedShapes(PathGroup.class)) {
            List<Path> paths = pathGroup.ungroup(drawing);
            paths.forEach(path -> path.setSelected(true));
            shapesChanged = true;
        }
        if (shapesChanged) {
            doChange(new ShapeListChange("Ungroup Paths", shapesBefore, drawing.saveShapes()));
            drawing.setDirty(true);
        }
        checkForChanges();
    }

    private InteractionEvent makeToolEvent(MouseEvent event, boolean restart) {
        Point2D point = canvas.mouseToGrid(event, true);
        Point2D mousePoint = canvas.mouseToGrid(event, false);
        double handleRadius = canvas.getSettings().getShapePointRadius() / canvas.getPixelsPerUnit();
        if (restart) {
            startPoint = point;
            mouseStartPoint = mousePoint;
            currentShape = null;
            currentHandle = null;
            for (Shape<?> shape : drawing.getShapes()) {
                currentHandle = shape.getHandle(point, mousePoint, handleRadius);
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
                currentShape, currentHandle,
                handleRadius);
    }

    private void checkSelectedShapes(Set<Shape<?>> selectedShapes) {
        // switch to recipe attached to selected shape
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
        updateClipboardMenuItems();
        updateGroupPathsMenuItems();
        drawing.setDirty(true);
    }

    private void checkSelectedShapes() {
        checkSelectedShapes(drawing.getSelectedShapes());
    }

    private void refreshDrawingWhenDirty() {
        if (drawing.isDirty()) {
            canvas.refresh();
            shapesTableController.syncShapes(drawing);
        }
    }

    private void checkForChanges() {
        checkSelectedShapes();
        refreshDrawingWhenDirty();
    }

    public void mousePressOnCanvas(MouseEvent event) {
        InteractionEvent toolEvent = makeToolEvent(event, true);
        if (currentTool != null) {
            currentShape = currentTool.down(toolEvent);
            if (currentTool.isSelectionTool()) {
                final Set<Shape<?>> selectionBefore = currentSelectedShapes;
                changeSupplier = () -> {
                    Set<Shape<?>> selectionAfter = drawing.getSelectedShapes();
                    if (selectionBefore.equals(selectionAfter)) {
                        return null;
                    } else {
                        return new SelectionChange("Select", drawing, selectionBefore, selectionAfter);
                    }
                };

                checkSelectedShapes();
            } else {
                // default behavior select current shape for non-selection tools
                changeSupplier = currentTool.prepareChange(drawing, currentShape);

                if (drawing.setSelectedShapes(currentShape)) {
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

            if (changeSupplier != null) {
                doChange(changeSupplier.get());
                changeSupplier = null;
            }

            if (currentTool.isSelectionTool()) {
                checkSelectedShapes();
            }
        }
        refreshDrawingWhenDirty();
    }

    public void setDrawing(Drawing newDrawing) {
        canvas.getDrawables().remove(drawing);
        drawing = newDrawing;
        unitCtl.setValue(newDrawing.getLengthUnit());
        canvas.getDrawables().add(newDrawing);

        shapesTableController.syncShapes(newDrawing);

        recipeEditorController.clearCurrentRecipe();
        recipeEditorController.getRecipes().clear();
        recipeEditorController.getRecipes().addAll(newDrawing.getRecipes());

        gCodeFileOperations.setCurrentFile(null);
        gCodeProgram = null;

        canvas.refresh();
    }

    private void updateInitialGCodeFileName(File drawingFile) {
        if (drawingFile == null) {
            gCodeFileOperations.getChooser().setInitialFileName(DEFAULT_GCODE_FILENAME);
        } else {
            String drawingFileName = drawingFile.getName();
            int extensionIndex = drawingFileName.lastIndexOf('.');
            if (extensionIndex > 0) {
                String gCodeFileName = drawingFileName.substring(0, extensionIndex) + ".nc";
                gCodeFileOperations.getChooser().setInitialFileName(gCodeFileName);
            }
        }
    }

    public void openDrawing() {
        Drawing newDrawing = drawingFileOperations.open();
        if (newDrawing != null) {
            setDrawing(newDrawing);
            updateInitialGCodeFileName(drawingFileOperations.getCurrentFile());
        }
    }

    public void saveDrawing() {
        drawingFileOperations.save(drawing);
        updateInitialGCodeFileName(drawingFileOperations.getCurrentFile());
    }

    public void saveDrawingAs() {
        drawingFileOperations.saveAs(drawing);
    }

    public void newDrawing() {
        Drawing newDrawing = new Drawing();
        newDrawing.setLengthUnit(unitCtl.getValue());
        setDrawing(newDrawing);
        updateInitialGCodeFileName(null);
    }

    public void saveImage() {
        GridSettings normalSettings = canvas.getSettings();
        GridSettings snapshotSettings = normalSettings.clone();
        snapshotSettings.setMajorGridPaint(Color.TRANSPARENT);
        snapshotSettings.setMinorGridPaint(Color.TRANSPARENT);
        snapshotSettings.setYAxisPaint(Color.TRANSPARENT);
        snapshotSettings.setXAxisPaint(Color.TRANSPARENT);
        snapshotSettings.setShapePaint(Color.BLACK);
        snapshotSettings.setSelectedShapePaint(Color.BLACK);
        snapshotSettings.setShapeLineWidth(2);
        snapshotSettings.setShapePointRadius(0);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.WHITE);
        canvas.setSettings(snapshotSettings);
        canvas.refresh();
        Image snapshot = canvas.snapshot(params, null);
        canvas.setSettings(normalSettings);
        canvas.refresh();
        imageFileOperations.saveAs(snapshot);
    }

    public void saveImageToFile(Image image, File imageFile, FileChooser.ExtensionFilter ext) throws IOException {
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
        ImageIO.write(bufferedImage, ext.getDescription(), imageFile);
    }

    public void closeWindow() {
        Stage stage = (Stage)rootPane.getScene().getWindow();
        stage.close();
    }

    private void updateGCodeMenuItems() {
        if (gCodeProgram != null) {
            saveGCodeItem.setDisable(false);
            saveGCodeAsItem.setDisable(false);
        } else {
            saveGCodeItem.setDisable(true);
            saveGCodeAsItem.setDisable(true);
        }
    }

    private void setGCodeProgram(GCodeProgram gCodeProgram) {
        this.gCodeProgram = gCodeProgram;

        if (gCodeProgram != null) {
            StringWriter gcodeWriter = new StringWriter();
            gCodeProgram.print(new PrintWriter(gcodeWriter));
            gcodeEditor.setText(gcodeWriter.toString());
        } else {
            gcodeEditor.setText("");
        }

        updateGCodeMenuItems();
    }

    public void generateGCode() {
        GCodeBuilder builder = new GCodeBuilder();
        builder .unitMode(drawing.getLengthUnit().getMode())
                .distanceMode(DistanceMode.ABSOLUTE)
                .arcDistanceMode(ArcDistanceMode.INCREMENTAL)
                .feedRateMode(FeedRateMode.UNITS_PER_MIN);

        for (Shape<?> shape : drawing.getShapes()) {
            int recipeId = shape.getRecipeId();
            if (recipeId <= 0) {
                continue;
            }

            GCodeRecipe recipe = drawing.getRecipe(recipeId).getRecipeForUnit(drawing.getLengthUnit());
            GCodeGenerator generator = recipe.getGCodeGenerator(shape);
            builder.emptyLine();
            if (generator != null) {
                builder.comment(String.format("shape:%s recipe:%s",
                        shape.getClass().getSimpleName(), recipe.getName()));
                generator.generateGCode(builder);
            } else {
                log.warn("Recipe:{} returned null generator for shape:{}", recipe, shape);
                builder.comment(String.format("shape:%s recipe:%s - no generator available",
                        shape.getClass().getSimpleName(), recipe.getName()));
            }
        }

        setGCodeProgram(builder.build());
    }

    public void saveGCode() {
        if (gCodeProgram != null) {
            gCodeFileOperations.save(gCodeProgram);
        }
    }

    public void saveGCodeAs() {
        if (gCodeProgram != null) {
            gCodeFileOperations.saveAs(gCodeProgram);
        }
    }

    private void updateClipboardMenuItems() {
        boolean noShapesOnClipboard = !Drawing.clipboardHasShapesContent(Clipboard.getSystemClipboard());
        pasteItem.setDisable(noShapesOnClipboard);

        boolean noShapesSelected = currentSelectedShapes.isEmpty();
        cutItem.setDisable(noShapesSelected);
        copyItem.setDisable(noShapesSelected);
        deleteItem.setDisable(noShapesSelected);
    }

    public void cut() {
        Snapshot<List<Shape<?>>> shapesBefore = drawing.saveShapes();
        drawing.saveSelectedShapesToClipboard(Clipboard.getSystemClipboard(), true);
        doChange(new ShapeListChange("Cut", shapesBefore, drawing.saveShapes()));
        checkForChanges();
    }

    public void copy() {
        drawing.saveSelectedShapesToClipboard(Clipboard.getSystemClipboard(), false);
        pasteItem.setDisable(false);
    }

    public void paste() {
        Snapshot<List<Shape<?>>> shapesBefore = drawing.saveShapes();
        drawing.addShapesFromClipboard(Clipboard.getSystemClipboard());
        doChange(new ShapeListChange("Paste", shapesBefore, drawing.saveShapes()));
        checkForChanges();
    }

    public void delete() {
        Snapshot<List<Shape<?>>> shapesBefore = drawing.saveShapes();
        drawing.removeAll(drawing.getSelectedShapes());
        doChange(new ShapeListChange("Delete", shapesBefore, drawing.saveShapes()));
        checkForChanges();
    }
}
