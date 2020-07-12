package com.gcodebuilder.geometry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcodebuilder.app.GridSettings;
import com.gcodebuilder.canvas.Drawable;
import com.gcodebuilder.recipe.GCodeRecipe;
import javafx.scene.canvas.GraphicsContext;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Drawing implements Drawable {
    private static final ObjectMapper OM = new ObjectMapper();

    @Getter
    private final List<Shape> shapes = new ArrayList<>();

    private final Map<Integer, GCodeRecipe> recipes = new HashMap<>();

    @Getter
    @Setter
    @JsonIgnore
    private boolean dirty = true;

    public void add(Shape shape) {
        shapes.add(shape);
        dirty = true;
    }

    public boolean remove(Shape shape) {
        if (shapes.remove(shape)) {
            dirty = true;
            return true;
        }
        return false;
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

    public List<GCodeRecipe> getRecipes() {
        return new ArrayList<>(recipes.values());
    }

    public void setRecipes(List<GCodeRecipe> recipes) {
        this.recipes.clear();
        recipes.forEach(this::putRecipe);
    }

    @JsonIgnore
    public Set<Shape> getSelectedShapes() {
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

    @Override
    public void draw(GraphicsContext ctx, double pixelsPerUnit, GridSettings settings) {
        for (Drawable shape : shapes) {
            shape.draw(ctx, pixelsPerUnit, settings);
        }
        dirty = false;
    }

    public void save(OutputStream out) throws IOException {
        OM.writeValue(out, this);
    }

    public String saveAsString() throws IOException {
        return OM.writeValueAsString(this);
    }

    public static Drawing load(InputStream in) throws IOException {
        return OM.readValue(in, Drawing.class);
    }

    public static Drawing load(String saved) throws IOException {
        return OM.readValue(saved, Drawing.class);
    }

    @Override
    public String toString() {
        return String.format("Drawing(shapes=%s, dirty=%s)", shapes, dirty);
    }
}
