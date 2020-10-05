package com.gcodebuilder.geometry;

import com.google.common.base.Preconditions;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

import java.util.List;

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

    /**
     * Compute an angle in radians from the components of a unit vector.
     *
     * @param unitX x coordinate of unit vector
     * @param unitY y coordinate of unit vector
     * @return angle in radians constrained to interval [0, 2*Math.PI]
     */
    public static double computeAngle(double unitX, double unitY) {
        double angle = Math.acos(unitX);
        if (unitY < 0) {
            angle = 2*Math.PI - angle;
        }
        return angle;
    }

    /**
     * Add two angles and constrain result to interval [0, 2*Math.PI].
     *
     * @param angleA one angle
     * @param angleB another angle
     * @return angleA in radians constrained to interval [0, 2*Math.PI]
     */
    public static double addAngle(double angleA, double angleB) {
        double result = angleA + angleB;
        while (result < 0) {
            result += 2*Math.PI;
        }
        while (result >= 2*Math.PI) {
            result -= 2*Math.PI;
        }
        return result;
    }

    /**
     * Subtract two angles and constrain result to interval [min, max) when minInclusive=true or (min, max] when
     * minInclusive=false. The difference between min and max should be equal to 2*Math.PI.
     *
     * @param angleA angle to subtract from (lhs)
     * @param angleB angle to subtract (rhs)
     * @param min minimum result
     * @param max maximum result
     * @param minInclusive true if min is inclusive; false if max is inclusive
     * @return
     */
    public static double subtractAngle(double angleA, double angleB, double min, double max, boolean minInclusive) {
        Preconditions.checkArgument(2*Math.PI == (max - min));
        double result = angleA - angleB;
        if (minInclusive) {
            while (result < min) {
                result += 2 * Math.PI;
            }
            while (result >= max) {
                result -= 2 * Math.PI;
            }
        } else {
            while (result <= min) {
                result += 2 * Math.PI;
            }
            while (result > max) {
                result -= 2 * Math.PI;
            }
        }
        return result;
    }

    /**
     * Subtract two angles and constrain result to interval (-Math.PI, Math.PI].
     *
     * The result will be the angle with the smallest absolute value that can be added to angleB using
     * {@link #addAngle(double, double)} to return angleA as the result.
     *
     * @param angleA
     * @param angleB
     * @return
     */
    public static double subtractAngle(double angleA, double angleB) {
        return subtractAngle(angleA, angleB, -Math.PI, Math.PI, false);
    }

    public static double convertToDegrees(double angleInRadians) {
        return angleInRadians * 180 / Math.PI;
    }

    public static double lengthSquared(double x, double y) {
        return (x * x) + (y * y);
    }

    public static double lengthSquared(Point2D vector) {
        return lengthSquared(vector.getX(), vector.getY());
    }

    public static Rectangle2D computeBoundingBox(List<Point> points) {
        Point first = points.get(0);
        double minX = first.getX();
        double maxX = minX;
        double minY = first.getY();
        double maxY = minY;
        for (int pointIndex = 1; pointIndex < points.size(); ++pointIndex) {
            Point point = points.get(pointIndex);
            minX = Math.min(minX, point.getX());
            maxX = Math.max(maxX, point.getX());
            minY = Math.min(minY, point.getY());
            maxY = Math.max(maxY, point.getY());
        }
        return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
    }

    public static double computeScaleFactor(double center, double original, double updated) {
        if (original > center) {
            updated = Math.max(updated, center);
            return 1.0 + (updated - original) / (original - center);
        } else if (original < center) {
            updated = Math.min(updated, center);
            return 1.0 + (original - updated) / (center - original);
        } else {
            return 0.0;
        }
    }

    public static double computeScaleFactor(Point2D center, Point2D original, Point2D updated) {
        return Math.max(
                computeScaleFactor(center.getX(), original.getX(), updated.getX()),
                computeScaleFactor(center.getY(), original.getY(), updated.getY()));
    }
}
