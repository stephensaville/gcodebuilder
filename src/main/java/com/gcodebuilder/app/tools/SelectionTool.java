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

import com.gcodebuilder.geometry.Shape;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SelectionTool implements Tool {
    private static final Logger log = LogManager.getLogger(SelectionTool.class);

    @Override
    public boolean isSelectionTool() {
        return true;
    }

    @Override
    public Shape<?> down(InteractionEvent event) {
        Shape<?> currentShape = event.getShape();
        if (event.getInputEvent().isControlDown()) {
            // update existing selection when control is down
            if (currentShape != null) {
                event.getShape().setSelected(!event.getShape().isSelected());
                event.getDrawing().setDirty(true);
                log.info("{} selection: {}",
                        event.getShape().isSelected() ? "Added to" : "Removed from",
                        currentShape);
            }
        } else if (event.getInputEvent().isShiftDown()) {
            // add to existing selection when shift is down
            if (currentShape != null) {
                event.getShape().setSelected(true);
                event.getDrawing().setDirty(true);
                log.info("Added to selection: " + currentShape);
            }
        } else {
            // reset selection when control is up
            if (event.getDrawing().setSelectedShapes(currentShape)) {
                event.getDrawing().setDirty(true);
                log.info("Set selection to only: " + currentShape);
            }
        }
        return currentShape;
    }

    @Override
    public void drag(InteractionEvent event) {

    }

    @Override
    public void up(InteractionEvent event) {
    }
}
