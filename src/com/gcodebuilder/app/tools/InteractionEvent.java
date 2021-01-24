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
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import lombok.Data;

@Data
public class InteractionEvent {
    private final Drawing drawing;
    private final MouseEvent inputEvent;
    private final Point2D point;
    private final Point2D startPoint;
    private final Point2D mousePoint;
    private final Point2D mouseStartPoint;
    private final Shape<?> shape;
    private final Object handle;
    private final double handleRadius;
}
