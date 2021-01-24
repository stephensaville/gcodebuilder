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

import com.gcodebuilder.geometry.Rectangle;

public class RectangleTool extends AddShapeTool<Rectangle> {
    @FunctionalInterface
    private interface RectangleFunction<T> {
        T apply(double minX, double minY, double width, double height);
    }

    private static <T> T eventToRect(InteractionEvent event, RectangleFunction<T> function) {
        double minX = Math.min(event.getPoint().getX(), event.getStartPoint().getX());
        double minY = Math.min(event.getPoint().getY(), event.getStartPoint().getY());
        double width = Math.abs(event.getPoint().getX() - event.getStartPoint().getX());
        double height = Math.abs(event.getPoint().getY() - event.getStartPoint().getY());
        return function.apply(minX, minY, width, height);
    }

    public RectangleTool() {
        super(Rectangle.class);
    }

    @Override
    protected Rectangle createShape(InteractionEvent event) {
        return eventToRect(event, Rectangle::new);
    }

    @Override
    protected boolean updateShape(InteractionEvent event, Rectangle currentShape) {
        return eventToRect(event, currentShape::update);
    }
}
