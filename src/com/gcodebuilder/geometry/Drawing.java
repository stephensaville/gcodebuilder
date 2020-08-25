package com.gcodebuilder.geometry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcodebuilder.app.GridSettings;
import com.gcodebuilder.canvas.Drawable;
import com.gcodebuilder.model.LengthUnit;
import com.gcodebuilder.recipe.GCodeRecipe;
import javafx.scene.canvas.GraphicsContext;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Drawing implements Drawable {
    private static final ObjectMapper OM = new ObjectMapper();

    @Getter
    @Setter
    private LengthUnit lengthUnit = LengthUnit.INCH;

    @Getter
    private final List<Shape<?>> shapes = new ArrayList<>();

    private final Map<Integer, GCodeRecipe> recipes = new HashMap<>();

    @Getter
    @Setter
    @JsonIgnore
    private boolean dirty = true;

    public void add(Shape<?> shape) {
        shapes.add(shape);
        dirty = true;
    }

    public void add(int index, Shape<?> shape) {
        int currentShapeIndex = shapes.indexOf(shape);
        if (currentShapeIndex < 0) {
            shapes.add(index, shape);
            dirty = true;
        } else if (currentShapeIndex < index) {
            shapes.remove(currentShapeIndex);
            shapes.add(index - 1, shape);
            dirty = true;
        } else if (currentShapeIndex > index) {
            shapes.remove(currentShapeIndex);
            shapes.add(index, shape);
            dirty = true;
        }
    }

    public boolean remove(Shape<?> shape) {
        if (shapes.remove(shape)) {
            dirty = true;
            return true;
        }
        return false;
    }

    public boolean contains(Shape<?> shape) {
        return shapes.contains(shape);
    }

    public GCodeRecipe getRecipe(int recipeId) {
        return recipes.get(recipeId);
    }

    public void putRecipe(GCodeRecipe recipe) {
        recipes.put(recipe.getId(), recipe);
    }

    public GCodeRecipe removeRecipe(int recipeId) {
        GCodeRecipe removed = recipes.remove(recipeId);
        if (removed != null) {
            shapes.forEach(shape -> {
                if (shape.getRecipeId() == removed.getId()) {
                    shape.setRecipeId(0);
                }
            });
        }
        return removed;
    }

    public boolean removeRecipe(GCodeRecipe recipe) {
        return removeRecipe(recipe.getId()) != null;
    }

    public List<GCodeRecipe> getRecipes() {
        return new ArrayList<>(recipes.values());
    }

    public void setRecipes(List<GCodeRecipe> recipes) {
        this.recipes.clear();
        recipes.forEach(this::putRecipe);
    }

    @JsonIgnore
    public Set<Shape<?>> getSelectedShapes() {
        return shapes.stream().filter(Shape::isSelected).collect(Collectors.toUnmodifiableSet());
    }

    public int unselectAllShapes() {
        return shapes.stream().mapToInt(shape -> {
            if (shape.isSelected()) {
                shape.setSelected(false);
                return 1;
            } else {
                return 0;
            }
        }).sum();
    }

    public boolean setSelectedShapes(Set<Shape<?>> selectedShapes) {
        boolean selectionChanged = false;
        for (Shape<?> shape : shapes) {
            boolean selected = selectedShapes.contains(shape);
            if (selected != shape.isSelected()) {
                shape.setSelected(selected);
                selectionChanged = true;
            }
        }
        return selectionChanged;
    }

    public boolean setSelectedShapes(Shape<?>... selectedShapes) {
        return setSelectedShapes(Arrays.asList(selectedShapes).stream()
                .filter(Objects::nonNull).collect(Collectors.toSet()));
    }

    @Override
    public void draw(GraphicsContext ctx, double pixelsPerUnit, GridSettings settings) {
        for (Drawable shape : shapes) {
            shape.draw(ctx, pixelsPerUnit, settings);
        }
        dirty = false;
    }

    public static void save(OutputStream out, Object object) throws IOException {
        OM.writeValue(out, object);
    }

    public void save(OutputStream out) throws IOException {
        save(out, this);
    }

    public static String saveAsString(Object object) throws IOException {
        return OM.writeValueAsString(object);
    }

    public String saveAsString() throws IOException {
        return saveAsString(this);
    }

    public static <T> T load(InputStream in, Class<T> type) throws IOException {
        return OM.readValue(in, type);
    }

    public static Drawing load(InputStream in) throws IOException {
        return load(in, Drawing.class);
    }

    public static <T> T loadFromString(String saved, Class<T> type) throws IOException {
        return OM.readValue(saved, type);
    }

    public static Drawing loadFromString(String saved) throws IOException {
        return loadFromString(saved, Drawing.class);
    }

    @Override
    public String toString() {
        return String.format("Drawing(shapes=%s, dirty=%s)", shapes, dirty);
    }
}
