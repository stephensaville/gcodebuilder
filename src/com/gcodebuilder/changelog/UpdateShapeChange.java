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

package com.gcodebuilder.changelog;

import com.gcodebuilder.geometry.Drawing;
import com.gcodebuilder.geometry.Shape;
import lombok.Data;

@Data
public class UpdateShapeChange implements Change {
    private final String description;
    private final Drawing drawing;
    private final Snapshot<? extends Shape<?>> before;
    private final Snapshot<? extends Shape<?>> after;

    @Override
    public void undo() {
        before.restore();
    }

    @Override
    public void redo() {
        after.restore();
    }
}
