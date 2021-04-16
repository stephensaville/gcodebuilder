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

package com.gcodebuilder.generator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GCodeDisplayMode {
    CONNECTED_SEGMENTS("Connected Segments"),
    SPLIT_POINTS("Split Points"),
    VALID_SEGMENTS("Valid Segments"),
    INSIDE_OUTSIDE("Inside/Outside"),
    TOOLPATHS("Profile Toolpaths"),
    ORIENTED_TOOLPATHS("Oriented Toolpaths"),
    POCKET_CONNECTED_SEGMENTS("Pocket Connected Segments"),
    POCKET_SPLIT_POINTS("Pocket Split Points"),
    POCKET_VALID_SEGMENTS("Pocket Valid Segments"),
    POCKET_INSIDE_OUTSIDE("Pocket Inside/Outside"),
    POCKET_TOOLPATHS("Pocket Toolpaths"),
    CONNECTED_TOOLPATHS("Connected Toolpaths");

    private final String label;
}
