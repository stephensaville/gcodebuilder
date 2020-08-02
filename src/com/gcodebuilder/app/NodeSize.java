package com.gcodebuilder.app;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public class NodeSize {
    static double measureHeight(Node child) {
        if (child == null) {
            return 0;
        }
        double height = child.getBoundsInParent().getHeight();
        Insets margin = BorderPane.getMargin(child);
        if (margin != null) {
            height += margin.getTop() + margin.getBottom();
        }
        return height;
    }

    static double measureWidth(Node child) {
        if (child == null) {
            return 0;
        }
        double width = child.getBoundsInParent().getWidth();
        Insets margin = BorderPane.getMargin(child);
        if (margin != null) {
            width += margin.getLeft() + margin.getRight();
        }
        return width;
    }
}
