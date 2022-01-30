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

package com.gcodebuilder.geometry;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ShapeIO {
    private static final Logger log = LogManager.getLogger(ShapeIO.class);

    private static final ObjectMapper OM = new ObjectMapper();

    public static void save(OutputStream out, Object object) throws IOException {
        OM.writeValue(out, object);
    }

    public static String saveAsString(Object object) throws IOException {
        return OM.writeValueAsString(object);
    }

    public static <T> T load(InputStream in, Class<T> type) throws IOException {
        return OM.readValue(in, type);
    }

    public static <T> T loadFromString(String saved, Class<T> type) throws IOException {
        return OM.readValue(saved, type);
    }

    public static void saveToClipboardContent(ClipboardContent content, DataFormat dataFormat, Object object) {
        try {
            content.put(dataFormat, saveAsString(object));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static ClipboardContent saveToClipboardContent(DataFormat dataFormat, Object object) {
        ClipboardContent content = new ClipboardContent();
        saveToClipboardContent(content, dataFormat, object);
        return content;
    }

    public static void saveToClipboard(Clipboard clipboard, DataFormat dataFormat, Object object) {
        clipboard.setContent(saveToClipboardContent(dataFormat, object));
    }

    public static <T> T loadFromClipboard(Clipboard clipboard, DataFormat dataFormat, Class<T> type) {
        if (clipboard.hasContent(dataFormat)) {
            try {
                return loadFromString((String)clipboard.getContent(dataFormat), type);
            } catch (Exception ex) {
                log.error("Failed to load content from clipboard!", ex);
            }
        }
        return null;
    }
}
