package com.gcodebuilder.app.recipe;

import com.gcodebuilder.recipe.GCodeRecipe;
import com.gcodebuilder.recipe.GCodeRecipeType;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.adapter.JavaBeanObjectProperty;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.collections.ModifiableObservableListBase;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import lombok.Data;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.NoSuchElementException;

public class RecipeEditorController {
    private static final Logger log = LogManager.getLogger(RecipeEditorController.class);

    @FXML
    private AnchorPane editorPane;

    @FXML
    private MenuButton recipeMenuBtn;

    @FXML
    private TextField recipeNameCtl;

    @Getter
    private final ObservableList<GCodeRecipe> recipes = new ModifiableObservableListBase<GCodeRecipe>() {
        private final List<GCodeRecipe> delegate = new ArrayList<>();

        @Override
        public GCodeRecipe get(int i) {
            return delegate.get(i);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        protected void doAdd(int i, GCodeRecipe recipe) {
            recipeMenuBtn.getItems().add(i, recipeMenuItem(recipe));
            delegate.add(i, recipe);
        }

        @Override
        protected GCodeRecipe doSet(int i, GCodeRecipe recipe) {
            MenuItem menuItem = recipeMenuItem(recipe);
            recipeMenuBtn.getItems().set(i, menuItem);
            GCodeRecipe previous = delegate.set(i, recipe);
            if (previous == currentRecipe) {
                setCurrentRecipe(recipe, menuItem);
            }
            return previous;
        }

        @Override
        protected GCodeRecipe doRemove(int i) {
            recipeMenuBtn.getItems().remove(i);
            GCodeRecipe removed = delegate.remove(i);
            if (removed == currentRecipe) {
                setCurrentRecipe(null, null);
            }
            return removed;
        }
    };

    @Getter
    private GCodeRecipe currentRecipe = null;
    private MenuItem currentRecipeMenuItem = null;

    private final JavaBeanObjectProperty<GCodeRecipe> currentRecipeProperty;

    @Data
    private static class RecipeTypeEditor {
        private final GCodeRecipeType type;
        private final Node node;
        private final RecipeTypeController controller;
    }

    private final EnumMap<GCodeRecipeType, RecipeTypeEditor> recipeTypeEditors =
            new EnumMap<>(GCodeRecipeType.class);
    private RecipeTypeEditor currentTypeEditor = null;

    public RecipeEditorController() {
        try {
            currentRecipeProperty = JavaBeanObjectPropertyBuilder.create().bean(this).name("currentRecipe").build();
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("Failed to create properties", ex);
        }
    }

    private void addRecipeTypeEditor(final GCodeRecipeType type,
                                     final Class<?> controllerClass,
                                     final String resourceName)
            throws IOException {
        FXMLLoader typeLoader = new FXMLLoader();
        Node node = typeLoader.load(controllerClass.getResourceAsStream(resourceName));
        AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setLeftAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, 0.0);
        RecipeTypeController controller = typeLoader.getController();
        recipeTypeEditors.put(type, new RecipeTypeEditor(type, node, controller));

        String menuItemText = String.format("Add %s Recipe", type.getLabel());
        MenuItem newRecipeMenuItem = new MenuItem(menuItemText);
        newRecipeMenuItem.setOnAction(event -> {
            addNewRecipe(type);
        });
        recipeMenuBtn.getItems().add(newRecipeMenuItem);
    }

    private MenuItem recipeMenuItem(GCodeRecipe recipe) {
        MenuItem menuItem = new MenuItem(recipe.getName());
        menuItem.setOnAction(event -> {
            setCurrentRecipe(recipe, menuItem);
        });
        return menuItem;
    }

    @FXML
    public void initialize() throws IOException {
        recipeNameCtl.textProperty().addListener((obs, oldValue, newValue) -> {
            setRecipeName(newValue);
        });
        recipeNameCtl.setDisable(true);

        addRecipeTypeEditor(
                GCodeRecipeType.PROFILE,
                ProfileRecipeEditorController.class,
                "profileRecipeEditor.fxml");

        addRecipeTypeEditor(
                GCodeRecipeType.POCKET,
                PocketRecipeEditorController.class,
                "pocketRecipeEditor.fxml");
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

    private void setCurrentRecipe(GCodeRecipe recipe, MenuItem recipeMenuItem) {
        currentRecipe = recipe;
        currentRecipeMenuItem = recipeMenuItem;
        if (recipe != null) {
            recipeMenuBtn.setText(currentRecipe.getName());
            recipeNameCtl.setText(currentRecipe.getName());
            recipeNameCtl.setDisable(false);
            RecipeTypeEditor editor = recipeTypeEditors.get(recipe.getType());
            if (editor != null) {
                editor.getController().setRecipe(recipe);
            }
            if (editor != currentTypeEditor) {
                if (currentTypeEditor != null) {
                    editorPane.getChildren().remove(currentTypeEditor.getNode());
                }
                if (editor != null) {
                    editorPane.getChildren().add(editor.getNode());
                }
            }
            currentTypeEditor = editor;
        } else {
            recipeMenuBtn.setText("None");
            recipeNameCtl.setText("");
            recipeNameCtl.setDisable(true);
            if (currentTypeEditor != null) {
                editorPane.getChildren().remove(currentTypeEditor.getNode());
            }
            currentTypeEditor = null;
        }
        currentRecipeProperty.fireValueChangedEvent();
    }

    public void setCurrentRecipe(GCodeRecipe recipe) {
        if (recipe == null) {
            clearCurrentRecipe();
            return;
        }
        int recipeIndex = recipes.indexOf(recipe);
        if (recipeIndex < 0) {
            throw new NoSuchElementException("recipe");
        }
        setCurrentRecipe(recipe, recipeMenuBtn.getItems().get(recipeIndex));
    }

    public void clearCurrentRecipe() {
        setCurrentRecipe(null, null);
    }

    public ObjectProperty<GCodeRecipe> currentRecipeProperty() {
        return currentRecipeProperty;
    }

    public void setRecipeName(String name) {
        if (currentRecipe != null && name != null && !name.equals(currentRecipe.getName())) {
            currentRecipe.setName(name);
            currentRecipeMenuItem.setText(name);
            recipeMenuBtn.setText(name);
        }
    }

    public void addNewRecipe(GCodeRecipeType type) {
        int maxRecipeId = 0;
        for (GCodeRecipe existingRecipe : recipes) {
            maxRecipeId = Math.max(maxRecipeId, existingRecipe.getId());
        }
        int newRecipeId = maxRecipeId + 1;
        GCodeRecipe newRecipe = type.newRecipe(newRecipeId);
        int recipeIndex = recipes.size();
        recipes.add(recipeIndex, newRecipe);
        setCurrentRecipe(newRecipe, recipeMenuBtn.getItems().get(recipeIndex));
    }
}
