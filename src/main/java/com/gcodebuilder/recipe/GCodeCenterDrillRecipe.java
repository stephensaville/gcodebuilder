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

package com.gcodebuilder.recipe;

import com.gcodebuilder.geometry.Point;
import com.gcodebuilder.geometry.Shape;

import java.util.Collections;
import java.util.List;

public class GCodeCenterDrillRecipe extends GCodeDrillingRecipe {
    public GCodeCenterDrillRecipe(int id) {
        super(id, GCodeRecipeType.CENTER_DRILL);
        setName(String.format("centerDrill%d", id));
    }

    @Override
    public List<Point> getDrillPoints(Shape<?> shape) {
        return Collections.singletonList(shape.getCenter());
    }
}
