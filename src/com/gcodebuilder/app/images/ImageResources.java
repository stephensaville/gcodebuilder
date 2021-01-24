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

package com.gcodebuilder.app.images;

import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ImageResources {
    private static final Logger log = LogManager.getLogger(ImageResources.class);

    public static Image load(String resourceName, boolean backgroundLoading) {
        String url = ImageResources.class.getResource(resourceName).toExternalForm();
        log.info("Loading image resource: {}", url);
        return new Image(url, backgroundLoading);
    }

    public static Image load(String resourceName) {
        return load(resourceName, false);
    }

    public static final String ICON_RESOURCE_PREFIX = "gcode_builder_icon";
    public static final String ICON_RESOURCE_SUFFIX = ".png";
    public static final int[] ICON_RESOURCE_SIZES = {64, 128, 256, 512, 1024};

    public static List<Image> loadIcons(boolean backgroundLoading) {
        return Arrays.stream(ICON_RESOURCE_SIZES)
                .mapToObj(size -> String.format("%s_%dx%d%s", ICON_RESOURCE_PREFIX, size, size, ICON_RESOURCE_SUFFIX))
                .map(name -> load(name, backgroundLoading))
                .collect(Collectors.toList());
    }

    public static List<Image> loadIcons() {
        return loadIcons(false);
    }
}
