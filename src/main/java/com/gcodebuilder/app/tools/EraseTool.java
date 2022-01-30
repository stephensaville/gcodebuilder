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
import com.gcodebuilder.changelog.RemoveShapeChange;
import com.gcodebuilder.changelog.Snapshot;
import com.gcodebuilder.changelog.Change;

import java.util.function.Supplier;

public class EraseTool implements Tool {
    @Override
    public Shape<?> down(InteractionEvent event) {
        if (event.getShape() != null) {
            event.getDrawing().remove(event.getShape());
        }
        return event.getShape();
    }

    @Override
    public void drag(InteractionEvent event) {

    }

    @Override
    public void up(InteractionEvent event) {

    }

    @Override
    public Supplier<Change> prepareChange(Drawing drawing, Shape<?> shape) {
        if (shape != null) {
            final Snapshot<? extends Shape<?>> before = shape.save();
            return () -> new RemoveShapeChange("Erase", drawing, before);
        } else {
            return null;
        }
    }
}
