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

import com.gcodebuilder.changelog.Change;
import com.gcodebuilder.changelog.Snapshot;
import com.gcodebuilder.changelog.UpdateShapeChange;
import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Math2D;
import com.gcodebuilder.geometry.Shape;
import javafx.geometry.Point2D;

import java.util.Formatter;
import java.util.function.Supplier;

public class MoveTool implements Tool {
    private Snapshot<? extends Shape<?>> original;
    private Point2D delta;

    @Override
    public Shape<?> down(InteractionEvent event) {
        Shape<?> shape = event.getShape();
        if (shape != null) {
            original = shape.save();
        } else {
            original = null;
        }
        delta = Point2D.ZERO;
        return shape;
    }

    private void moveShape(InteractionEvent event) {
        if (original != null) {
            Point2D newDelta = event.getPoint().subtract(event.getStartPoint());
            boolean moved;
            if (Math2D.samePoints(delta, newDelta)) {
                moved = false;
            } else {
                Shape<?> shape = original.restore();
                shape.move(newDelta);
                delta = newDelta;
                moved = true;
            }
            event.getDrawing().setDirty(moved);
        }
    }

    @Override
    public void drag(InteractionEvent event) {
        moveShape(event);
    }

    @Override
    public void up(InteractionEvent event) {
        moveShape(event);
    }

    @Override
    public Supplier<Change> prepareChange(Drawing drawing, Shape<?> shape) {
        if (shape != null) {
            return () -> {
                if (Math2D.samePoints(delta, Point2D.ZERO)) {
                    return null;
                } else {
                    return new UpdateShapeChange("Move", drawing, original, shape.save());
                }
            };
        } else {
            return null;
        }
    }

    @Override
    public void addStatusText(InteractionEvent event, Formatter statusFormatter) {
        Point2D delta = event.getPoint().subtract(event.getStartPoint());
        statusFormatter.format("  dx: %.4f  dy: %.4f", delta.getX(), delta.getY());
    }
}
