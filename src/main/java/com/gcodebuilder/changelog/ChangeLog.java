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

import java.util.ArrayList;
import java.util.List;

public class ChangeLog {
    private final List<Change> changes = new ArrayList<>();
    private int changeIndex = 0;

    public void doChange(Change change) {
        if (changeIndex < changes.size()) {
            // NOTE: this is a branch point
            changes.subList(changeIndex, changes.size()).clear();
        }
        changes.add(change);
        changeIndex = changes.size();
    }

    public void undoChange() {
        if (changeIndex > 0) {
            changes.get(--changeIndex).undo();
        }
    }

    public boolean isUndoEnabled() {
        return changeIndex > 0;
    }

    public String getUndoDescription() {
        if (changeIndex > 0) {
            return changes.get(changeIndex - 1).getDescription();
        } else {
            return null;
        }
    }

    public void redoChange() {
        if (changeIndex < changes.size()) {
            changes.get(changeIndex++).redo();
        }
    }

    public boolean isRedoEnabled() {
        return changeIndex < changes.size();
    }

    public String getRedoDescription() {
        if (changeIndex < changes.size()) {
            return changes.get(changeIndex).getDescription();
        } else {
            return null;
        }
    }
}
