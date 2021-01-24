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
import com.gcodebuilder.changelog.Change;
import javafx.geometry.Rectangle2D;

import java.util.Formatter;
import java.util.function.Supplier;

public interface Tool {
    Shape<?> down(InteractionEvent event);
    void drag(InteractionEvent event);
    void up(InteractionEvent event);

    default boolean isSelectionTool() {
        return false;
    }

    default Supplier<Change> prepareChange(Drawing drawing, Shape<?> shape) {
        return null;
    }

    default void addStatusText(InteractionEvent event, Formatter statusFormatter) {
        Shape<?> shape = event.getShape();
        if (shape != null) {
            Rectangle2D boundingBox = shape.getBoundingBox();
            statusFormatter.format("  w: %.4f  h: %.4f",
                    boundingBox.getWidth(), boundingBox.getHeight());
        }
    }
}
