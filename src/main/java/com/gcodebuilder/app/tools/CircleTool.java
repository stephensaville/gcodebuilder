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

import com.gcodebuilder.geometry.Circle;
import com.gcodebuilder.geometry.Point;

public class CircleTool extends AddShapeTool<Circle> {
    @FunctionalInterface
    private interface CircleFunction<T> {
        T apply(Point center, double radius);
    }

    private static <T> T eventToCircle(InteractionEvent event, CircleFunction<T> function) {
        Point center = new Point(event.getStartPoint());
        double radius = center.distance(event.getPoint());
        return function.apply(center, radius);
    }

    public CircleTool() {
        super(Circle.class);
    }

    @Override
    protected Circle createShape(InteractionEvent event) {
        return eventToCircle(event, Circle::new);
    }

    @Override
    protected boolean updateShape(InteractionEvent event, Circle currentShape) {
        return eventToCircle(event, currentShape::update);
    }
}
