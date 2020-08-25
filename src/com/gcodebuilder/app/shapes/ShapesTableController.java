package com.gcodebuilder.app.shapes;

import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class ShapesTableController {
    private static final Logger log = LogManager.getLogger(ShapesTableController.class);

    private static final Paint DRAG_COLOR = Color.BLUE;

    private static final Border DRAG_DONE_BORDER = new Border(new BorderStroke(
            DRAG_COLOR, DRAG_COLOR, DRAG_COLOR, DRAG_COLOR,
            BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.NONE,
            CornerRadii.EMPTY,
            new BorderWidths(2.0, 0.0, 2.0, 0.0),
            new Insets(0.0)));

    private static final Border DRAG_ABOVE_BORDER = new Border(new BorderStroke(
            DRAG_COLOR, DRAG_COLOR, DRAG_COLOR, DRAG_COLOR,
            BorderStrokeStyle.SOLID, BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.NONE,
            CornerRadii.EMPTY,
            new BorderWidths(2.0, 0.0, 2.0, 0.0),
            new Insets(0.0)));

    private static final Border DRAG_BELOW_BORDER = new Border(new BorderStroke(
            DRAG_COLOR, DRAG_COLOR, DRAG_COLOR, DRAG_COLOR,
            BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.SOLID, BorderStrokeStyle.NONE,
            CornerRadii.EMPTY,
            new BorderWidths(2.0, 0.0, 2.0, 0.0),
            new Insets(0.0)));

    @FXML
    private TableView<ObservableShape> shapeTable;

    @FXML
    private TableColumn<ObservableShape, String> shapeTypeColumn;

    @FXML
    private TableColumn<ObservableShape, String> recipeNameColumn;

    private final ObservableList<ObservableShape> shapes = FXCollections.observableArrayList();

    private Drawing drawing;

    private boolean isDragBelowCenter(TableCell<?, ?> tableCell, DragEvent event) {
        if (tableCell.getIndex() == tableCell.getTableView().getItems().size() - 1) {
            Bounds boundsInScene = tableCell.getLocalToSceneTransform().transform(
                    tableCell.getBoundsInLocal());
            return event.getSceneY() > boundsInScene.getCenterY();
        } else {
            return false;
        }
    }

    private int getDropIndex(TableCell<?, ?> tableCell, DragEvent event) {
        int dropIndex = tableCell.getIndex();
        if (isDragBelowCenter(tableCell, event)) {
            ++dropIndex;
        }
        return dropIndex;
    }

    @FXML
    public void initialize() {
        shapeTypeColumn.setCellValueFactory(cdf -> cdf.getValue().shapeTypeProperty());
        shapeTypeColumn.setCellFactory(column -> {
            final TextFieldTableCell<ObservableShape, String> tableCell = new TextFieldTableCell<>();
            tableCell.setOnDragDetected(event -> {
                Dragboard dragboard = tableCell.startDragAndDrop(TransferMode.MOVE);
                Shape<?> shape = tableCell.getTableRow().getItem().getShape();
                dragboard.setContent(shape.getClipboardContent());
                event.consume();
            });
            tableCell.setOnDragOver(event -> {
                Dragboard dragboard = event.getDragboard();
                if (Shape.clipboardHasShapeContent(dragboard)) {
                    event.acceptTransferModes(TransferMode.MOVE);
                    if (isDragBelowCenter(tableCell, event)) {
                        tableCell.getTableRow().setBorder(DRAG_BELOW_BORDER);
                    } else {
                        tableCell.getTableRow().setBorder(DRAG_ABOVE_BORDER);
                    }
                }
                event.consume();
            });
            tableCell.setOnDragExited(event -> {
                tableCell.getTableRow().setBorder(DRAG_DONE_BORDER);
            });
            tableCell.setOnDragDropped(event -> {
                Dragboard dragboard = event.getDragboard();
                boolean success = false;
                Shape<?> shape = Shape.getShapeFromClipboard(dragboard, drawing);
                if (shape != null) {
                    drawing.add(getDropIndex(tableCell, event), shape);
                    syncShapes(drawing);
                    selectShape(shape, drawing);
                    success = true;
                }
                event.setDropCompleted(success);
                event.consume();
            });
            return tableCell;
        });
        shapeTypeColumn.setEditable(false);

        recipeNameColumn.setCellValueFactory(cdf -> cdf.getValue().recipeNameProperty());

        final Callback<TableView<ObservableShape>, TableRow<ObservableShape>> defaultRowFactory =
                shapeTable.getRowFactory();
         shapeTable.setRowFactory(tableView -> {
             TableRow<ObservableShape> tableRow = new TableRow<>();
             tableRow.setBorder(DRAG_DONE_BORDER);
             return tableRow;
         });

        shapeTable.setItems(shapes);
        shapeTable.setSortPolicy(table -> false);
    }

    public static ShapesTableController attach(TitledPane parent) throws IOException {
        FXMLLoader shapeTableLoader = new FXMLLoader();
        Node shapeTable = shapeTableLoader.load(
                ShapesTableController.class.getResourceAsStream("shapeTable.fxml"));
        parent.setContent(shapeTable);
        return shapeTableLoader.getController();
    }

    public void syncShapes(Drawing drawing) {
        this.drawing = drawing;
        int shapeIndex = 0;
        boolean shapeCountChanged = false;
        for (Shape<?> shape : drawing.getShapes()) {
            if (shapeIndex == shapes.size()) {
                shapes.add(new ObservableShape(shape, drawing));
                shapeCountChanged = true;
            } else {
                ObservableShape current = shapes.get(shapeIndex);
                if (!current.shapeEquals(shape, drawing)) {
                    shapes.set(shapeIndex, new ObservableShape(shape, drawing));
                } else {
                    current.syncProperties();
                }
            }
            if (shape.isSelected()) {
                shapeTable.getSelectionModel().select(shapeIndex);
            } else {
                shapeTable.getSelectionModel().clearSelection(shapeIndex);
            }
            ++shapeIndex;
        }
        if (shapeIndex < shapes.size()) {
            shapes.subList(shapeIndex, shapes.size()).clear();
            shapeCountChanged = true;
        }
        if (shapeCountChanged) {
            shapeTable.setPrefHeight(28 + shapeTable.getFixedCellSize()*Math.max(1, shapes.size()));
        }
    }

    public void syncShape(Shape<?> shape, Drawing drawing) {
        for (ObservableShape observableShape : shapes) {
            if (observableShape.shapeEquals(shape, drawing)) {
                observableShape.syncProperties();
            }
        }
    }

    public void selectShape(Shape<?> shape, Drawing drawing) {
        int shapeIndex = 0;
        for (ObservableShape observableShape : shapes) {
            if (observableShape.shapeEquals(shape, drawing)) {
                shapeTable.getSelectionModel().select(shapeIndex);
            }
            ++shapeIndex;
        }
    }

    public MultipleSelectionModel<ObservableShape> getSelectedShapes() {
        return shapeTable.getSelectionModel();
    }
}
