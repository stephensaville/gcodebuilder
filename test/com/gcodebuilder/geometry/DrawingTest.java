package com.gcodebuilder.geometry;

import javafx.geometry.Point2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;


public class DrawingTest {
    private static final Logger log = LogManager.getLogger(DrawingTest.class);

    private static void assertEquivalentDrawing(Drawing expected, Drawing actual) {
        Assertions.assertEquals(expected.getShapes(), actual.getShapes());
    }

    private void saveTest(Drawing original) throws IOException {
        log.debug("original: {}", original);
        String saved = original.saveAsString();
        log.debug("saved: {}", saved);
        Drawing loaded = Drawing.load(saved);
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
        original.add(new Circle(new Point2D(1, 1), 2));
        saveTest(original);
    }

    @Test
    public void testSaveDrawingWithAllShapes() throws IOException {
        Drawing original = new Drawing();
        original.add(new Rectangle(1, 2, 3, 4));
        original.add(new Circle(new Point2D(1, 2), 3));
        saveTest(original);
    }
}
