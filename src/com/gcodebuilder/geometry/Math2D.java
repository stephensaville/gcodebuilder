package com.gcodebuilder.geometry;

import javafx.geometry.Point2D;

public class Math2D {
    /**
     * Calculates the determinant of the 2x2 matrix defined as:
     *
     *                   | a b |
     * det(a, b, c, d) = | c d | = ad - bc
     *
     * @param a top left corner
     * @param b top right corner
     * @param c bottom left corner
     * @param d bottom right corner
     * @return determinant of 2x2 matrix
     */
    public static double det(double a, double b, double c, double d) {
        return a * d - b * c;
    }

    /**
     * Calculates the determine of the 2x2 matrix defined as:
     *
     *               | p1.x p2.x |
     * det(p1, p2) = | p1.y p2.y | = p1.x * p2.y - p2.x * p1.y
     *
     * @param p1 defines left column of matrix
     * @param p2 defines right column of matrix
     * @return determinant of 2x2 matrix
     */
    public static double det(Point2D p1, Point2D p2) {
        return det(p1.getX(), p2.getX(), p1.getY(), p2.getY());
    }
}
