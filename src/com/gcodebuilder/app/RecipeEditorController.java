package com.gcodebuilder.app;

import com.gcodebuilder.recipe.GCodeRecipe;
import com.gcodebuilder.recipe.GCodeRecipeType;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import lombok.Data;
import org.apache.batik.bridge.TextNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.EnumMap;

public class RecipeEditorController {
    private static final Logger log = LogManager.getLogger(RecipeEditorController.class);

    @FXML
    private AnchorPane editorPane;

    @FXML
    private ComboBox<GCodeRecipe> recipeCtl;

    @FXML
    private TextField recipeNameCtl;

    @FXML
    private Button newRecipeBtn;

    @FXML
    private ComboBox<GCodeRecipeType> recipeTypeCtl;

    @Data
    private static class RecipeTypeEditor {
        private final GCodeRecipeType type;
        private final Node node;
        private final RecipeTypeController controller;
    }

    private final EnumMap<GCodeRecipeType, RecipeTypeEditor> recipeTypeEditors = new EnumMap<>(GCodeRecipeType.class);
    private RecipeTypeEditor currentTypeEditor = null;

    private void addRecipeTypeEditor(GCodeRecipeType type, Class<?> controllerClass, String resourceName)
            throws IOException {
        FXMLLoader typeLoader = new FXMLLoader();
        Node node = typeLoader.load(controllerClass.getResourceAsStream(resourceName));
        AnchorPane.setTopAnchor(node, 5.0);
        AnchorPane.setLeftAnchor(node, 5.0);
        AnchorPane.setRightAnchor(node, 5.0);
        RecipeTypeController controller = typeLoader.getController();
        recipeTypeEditors.put(type, new RecipeTypeEditor(type, node, controller));
        recipeTypeCtl.getItems().add(type);
    }

    @FXML
    public void initialize() throws IOException {
        recipeNameCtl.textProperty().addListener((obs, oldValue, newValue) -> {
            GCodeRecipe currentRecipe = getCurrentRecipe();
            if (currentRecipe != null && !newValue.equals(currentRecipe.getName())) {
                currentRecipe.setName(newValue);
                recipeCtl.setValue(currentRecipe);
            }
        });

        addRecipeTypeEditor(
                GCodeRecipeType.PROFILE,
                ProfileRecipeEditorController.class,
                "profileRecipeEditor.fxml");
        setCurrentRecipeType(GCodeRecipeType.PROFILE);
    }

    public static RecipeEditorController attach(AnchorPane parent) throws IOException {
        FXMLLoader recipeEditorLoader = new FXMLLoader();
        Node recipeEditor = recipeEditorLoader.load(
                RecipeEditorController.class.getResourceAsStream("recipeEditor.fxml"));
        AnchorPane.setTopAnchor(recipeEditor, 5.0);
        AnchorPane.setLeftAnchor(recipeEditor, 5.0);
        AnchorPane.setRightAnchor(recipeEditor, 5.0);
        parent.getChildren().add(recipeEditor);
        return recipeEditorLoader.getController();
    }

    public ObservableList<GCodeRecipe> getRecipeList() {
        return recipeCtl.getItems();
    }

    public GCodeRecipe getCurrentRecipe() {
        return recipeCtl.getValue();
    }

    public void setCurrentRecipe(GCodeRecipe recipe) {
        recipeCtl.setValue(recipe);
        setCurrentRecipeType(recipe.getType());
    }

    public void recipeChanged() {
        GCodeRecipe recipe = getCurrentRecipe();
        log.info("Current recipe changed: " + getCurrentRecipe());
        recipeNameCtl.setText(recipe.getName());
        setCurrentRecipeType(recipe.getType());
    }

    public GCodeRecipeType getCurrentRecipeType() {
        return recipeTypeCtl.getValue();
    }

    public void setCurrentRecipeType(GCodeRecipeType type) {
        recipeTypeCtl.setValue(type);
        RecipeTypeEditor editor = recipeTypeEditors.get(type);
        GCodeRecipe currentRecipe = getCurrentRecipe();
        if (currentRecipe != null && currentRecipe.getType() == type) {
            editor.getController().setRecipe(currentRecipe);
            if (editor != currentTypeEditor) {
                if (currentTypeEditor != null) {
                    editorPane.getChildren().remove(currentTypeEditor.getNode());
                }
                if (editor != null) {
                    editorPane.getChildren().add(editor.getNode());
                }
            }
            currentTypeEditor = editor;
        } else if (currentTypeEditor != null) {
            editorPane.getChildren().remove(currentTypeEditor.getNode());
            currentTypeEditor = null;
        }
    }

    public void recipeTypeChanged() {
        GCodeRecipeType recipeType = getCurrentRecipeType();
        log.info("Recipe type changed: " + recipeType);
    }

    public void addNewRecipe() {
        int maxRecipeId = 0;
        for (GCodeRecipe existingRecipe : getRecipeList()) {
            maxRecipeId = Math.max(maxRecipeId, existingRecipe.getId());
        }
        int newRecipeId = maxRecipeId + 1;
        GCodeRecipeType newRecipeType = getCurrentRecipeType();
        GCodeRecipe newRecipe = newRecipeType.newRecipe(newRecipeId);
        getRecipeList().add(newRecipe);
        setCurrentRecipe(newRecipe);
    }
}
