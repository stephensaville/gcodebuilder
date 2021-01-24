/*
 * Copyright (c) 2021 Stephen Saville
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gcodebuilder.geometry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gcodebuilder.app.GridSettings;
import com.gcodebuilder.canvas.Drawable;
import com.gcodebuilder.changelog.Snapshot;
import com.gcodebuilder.model.LengthUnit;
import com.gcodebuilder.recipe.GCodeRecipe;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.Clipboard;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Drawing implements Drawable {
    private static final Logger log = LogManager.getLogger(Drawing.class);

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

    public boolean addAll(Collection<? extends Shape<?>> shapes) {
        boolean changed = this.shapes.addAll(shapes);
        if (changed) {
            dirty = true;
        }
        return changed;
    }

    public boolean remove(Shape<?> shape) {
        if (shapes.remove(shape)) {
            dirty = true;
            return true;
        }
        return false;
    }

    public boolean removeAll(Collection<? extends Shape<?>> shapes) {
        boolean changed = this.shapes.removeAll(shapes);
        if (changed) {
            dirty = true;
        }
        return changed;
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

    public boolean hasSelectedShapes() {
        return shapes.stream().anyMatch(Shape::isSelected);
    }

    @JsonIgnore
    public Set<Shape<?>> getSelectedShapes() {
        return shapes.stream()
                .filter(Shape::isSelected)
                .collect(Collectors.toUnmodifiableSet());
    }

    public <T extends Shape<?>> Set<T> getSelectedShapes(Class<T> shapeClass) {
        return shapes.stream()
                .filter(Shape::isSelected)
                .filter(shapeClass::isInstance)
                .map(shapeClass::cast)
                .collect(Collectors.toUnmodifiableSet());
    }

    @JsonIgnore
    public Shape<?> getSelectedShape() {
        Set<Shape<?>> selectedShapes = getSelectedShapes();
        if (selectedShapes.size() == 1) {
            return selectedShapes.iterator().next();
        }
        return null;
    }

    public <T extends Shape<?>> T getSelectedShape(Class<T> shapeClass) {
        Shape<?> selectedShape = getSelectedShape();
        if (shapeClass.isInstance(selectedShape)) {
            return shapeClass.cast(selectedShape);
        }
        return null;
    }

    public boolean setSelectedShapes(Collection<Shape<?>> selectedShapes) {
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

    public void saveSelectedShapesToClipboard(Clipboard clipboard, boolean removeShapes) {
        Set<Shape<?>> selectedShapes = getSelectedShapes();
        if (selectedShapes.size() == 1) {
            Shape<?> singleShape = selectedShapes.iterator().next();
            singleShape.saveToClipboard(clipboard);
        } else if (selectedShapes.size() > 1) {
            ShapeList shapeList = new ShapeList(selectedShapes);
            shapeList.saveToClipboard(clipboard);
        }
        if (removeShapes) {
            removeAll(selectedShapes);
        }
    }

    public static boolean clipboardHasShapesContent(Clipboard clipboard) {
        log.info("Checking clipboard for shapes content: {}", clipboard.getContentTypes());
        return Shape.clipboardHasContent(clipboard, false)
                || ShapeList.clipboardHasContent(clipboard);
    }

    public void addShapesFromClipboard(Clipboard clipboard) {
        if (Shape.clipboardHasContent(clipboard, false)) {
            Shape<?> shapeFromClipboard = Shape.loadFromClipboard(clipboard, this, false);
            if (shapeFromClipboard != null) {
                add(shapeFromClipboard);
                setSelectedShapes(shapeFromClipboard);
            }
        } else if (ShapeList.clipboardHasContent(clipboard)) {
            ShapeList shapeListFromClipboard = ShapeList.loadFromClipboard(clipboard);
            if (shapeListFromClipboard != null) {
                addAll(shapeListFromClipboard.getShapes());
                setSelectedShapes(shapeListFromClipboard.getShapes());
            }
        }
    }

    public Snapshot<List<Shape<?>>> saveShapes() {
        return new Snapshot<>() {
            private final List<Shape<?>> shapes = new ArrayList<>(getShapes());

            @Override
            public List<Shape<?>> restore() {
                Drawing.this.shapes.clear();
                Drawing.this.shapes.addAll(shapes);
                return shapes;
            }
        };
    }

    @Override
    public void draw(GraphicsContext ctx, double pixelsPerUnit, GridSettings settings) {
        for (Drawable shape : shapes) {
            shape.draw(ctx, pixelsPerUnit, settings);
        }
        dirty = false;
    }

    public void save(OutputStream out) throws IOException {
        ShapeIO.save(out, this);
    }

    public String saveAsString() throws IOException {
        return ShapeIO.saveAsString(this);
    }

    public static Drawing load(InputStream in) throws IOException {
        return ShapeIO.load(in, Drawing.class);
    }

    public static Drawing loadFromString(String saved) throws IOException {
        return ShapeIO.loadFromString(saved, Drawing.class);
    }

    @Override
    public String toString() {
        return String.format("Drawing(shapes=%s, dirty=%s)", shapes, dirty);
    }
}
