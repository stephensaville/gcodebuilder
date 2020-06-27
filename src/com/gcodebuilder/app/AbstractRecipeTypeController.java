package com.gcodebuilder.app;

import com.gcodebuilder.app.recipe.RecipeTypeController;
import com.gcodebuilder.recipe.GCodeRecipe;
import com.gcodebuilder.recipe.GCodeRecipeType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractRecipeTypeController<T extends GCodeRecipe> implements RecipeTypeController {
    private static final DoubleStringConverter DOUBLE_CONVERTER = new DoubleStringConverter();
    protected static TextFormatter<Double> doubleFormatter(double defaultValue) {
        return new TextFormatter<>(DOUBLE_CONVERTER, defaultValue);
    }
    protected static TextFormatter<Double> doubleFormatter() {
        return doubleFormatter(0);
    }

    private static final IntegerStringConverter INTEGER_CONVERTER = new IntegerStringConverter();
    protected static TextFormatter<Integer> integerFormatter(int defaultValue) {
        return new TextFormatter<>(INTEGER_CONVERTER, defaultValue);
    }
    protected static TextFormatter<Integer> integerFormatter() {
        return integerFormatter(0);
    }

    @Getter
    private final GCodeRecipeType recipeType;

    @Getter
    private final Class<T> recipeClass;

    private T recipe;

    private final List<Consumer<T>> settingAppliers = new ArrayList<>();

    protected <V> void configuredChoiceBox(ChoiceBox<V> choiceBox,
                                         Function<T, V> getter,
                                         BiConsumer<T, V> setter,
                                         V... values) {
        choiceBox.getItems().addAll(values);
        choiceBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            setter.accept(getRecipe(), newValue);
        });
        settingAppliers.add((recipe) -> {
            choiceBox.setValue(getter.apply(recipe));
        });
    }

    protected <V> void configureTextField(TextField field,
                                        TextFormatter<V> formatter,
                                        Function<T, V> getter,
                                        BiConsumer<T, V> setter) {
        field.setTextFormatter(formatter);
        formatter.valueProperty().addListener((obs, oldValue, newValue) -> {
            setter.accept(getRecipe(), newValue);
        });
        settingAppliers.add((recipe) -> {
            formatter.setValue(getter.apply(recipe));
        });
    }

    public AbstractRecipeTypeController(GCodeRecipeType recipeType, Class<T> recipeClass) {
        this.recipeType = recipeType;
        this.recipeClass = recipeClass;
    }

    @Override
    public T getRecipe() {
        return recipe;
    }

    public void setRecipe(GCodeRecipe recipe) {
        internalSetRecipe(recipeClass.cast(recipe));
    }

    private void internalSetRecipe(T recipe) {
        if (recipe == null) {
            throw new NullPointerException("recipe");
        }
        if (this.recipe == recipe) {
            return;
        }
        this.recipe = recipe;
        settingAppliers.forEach(applier -> applier.accept(recipe));
    }
}
