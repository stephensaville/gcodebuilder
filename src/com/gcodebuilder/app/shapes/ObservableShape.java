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

package com.gcodebuilder.app.shapes;

import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Data;

import java.util.Objects;

@Data
public class ObservableShape {
    private final Shape<?> shape;
    private final Drawing drawing;

    private SimpleStringProperty shapeType;
    private SimpleStringProperty recipeName;

    public ObservableShape(Shape<?> shape, Drawing drawing) {
        this.shape = shape;
        this.drawing = drawing;
        shapeType = new SimpleStringProperty(this, "shapeType", shape.getClass().getSimpleName());
        recipeName = new SimpleStringProperty(this, "recipeName");
        syncProperties();
    }

    public boolean shapeEquals(Shape<?> shape, Drawing drawing) {
        return Objects.equals(shape, this.shape) && Objects.equals(drawing, this.drawing);
    }

    public ReadOnlyProperty<String> shapeTypeProperty() {
        return shapeType;
    }

    public ReadOnlyProperty<String> recipeNameProperty() {
        return recipeName;
    }

    public void syncProperties() {
        if (shape.getRecipeId() > 0) {
            recipeName.set(drawing.getRecipe(shape.getRecipeId()).getName());
        } else {
            recipeName.set("");
        }
    }
}
