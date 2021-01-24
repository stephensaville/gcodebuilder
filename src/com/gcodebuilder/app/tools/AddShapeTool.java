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

package com.gcodebuilder.app.tools;

import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import com.gcodebuilder.changelog.AddShapeChange;
import com.gcodebuilder.changelog.Change;

import java.util.function.Supplier;

public abstract class AddShapeTool<S extends Shape<?>> implements Tool {
    private final Class<S> shapeClass;

    protected AddShapeTool(Class<S> shapeClass) {
        this.shapeClass = shapeClass;
    }

    protected abstract S createShape(InteractionEvent event);

    protected abstract boolean updateShape(InteractionEvent event, S currentShape);

    @Override
    public S down(InteractionEvent event) {
        S newShape = createShape(event);
        event.getDrawing().add(newShape);
        return newShape;
    }

    private S updateCurrentShape(InteractionEvent event) {
        S currentShape = shapeClass.cast(event.getShape());
        if (updateShape(event, currentShape)) {
            event.getDrawing().setDirty(true);
        }
        return currentShape;
    }

    @Override
    public void drag(InteractionEvent event) {
        updateCurrentShape(event);
    }

    @Override
    public void up(InteractionEvent event) {
        S shape = updateCurrentShape(event);
        if (!shape.isVisible()) {
            event.getDrawing().remove(shape);
        }
    }

    @Override
    public Supplier<Change> prepareChange(final Drawing drawing, final Shape<?> shape) {
        return () -> {
            if (shape.isVisible()) {
                return new AddShapeChange(shapeClass.getSimpleName(), drawing, shape.save());
            } else {
                return null;
            }
        };
    }
}
