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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


public class DrawingTest {
    private static final Logger log = LogManager.getLogger(DrawingTest.class);

    private static void assertEquivalentDrawing(Drawing expected, Drawing actual) {
        Assertions.assertEquals(expected.getShapes(), actual.getShapes());
    }

    private void saveTest(Drawing original) throws IOException {
        log.debug("original: {}", original);
        String saved = original.saveAsString();
        log.debug("saved: {}", saved);
        Drawing loaded = Drawing.loadFromString(saved);
        log.debug("loaded: {}", loaded);
        assertEquivalentDrawing(original, loaded);
    }

    @Test
    public void testSaveEmptyDrawing() throws IOException {
        saveTest(new Drawing());
    }

    @Test
    public void testSaveDrawingWithRectangle() throws IOException {
        Drawing original = new Drawing();
        original.add(new Rectangle(-1, -1, 2, 2));
        saveTest(original);
    }

    @Test
    public void testSaveDrawingWithCircle() throws IOException {
        Drawing original = new Drawing();
        original.add(new Circle(new Point(1, 1), 2));
        saveTest(original);
    }

    @Test
    public void testSaveDrawingWithAllShapes() throws IOException {
        Drawing original = new Drawing();
        original.add(new Rectangle(1, 2, 3, 4));
        original.add(new Circle(new Point(1, 2), 3));
        saveTest(original);
    }

    @Test
    public void testOpenDrawingFromFile() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("test_all_shapes.json")) {
            Drawing loaded = Drawing.load(in);
            Assertions.assertNotNull(loaded);
            log.info("loaded from file: {}", loaded);
        }
    }
}
