package com.gcodebuilder.geometry;

import javafx.geometry.Point2D;
import lombok.Getter;

@Getter
public class UnitVector extends Point2D {
    private final double angle;

    public static UnitVector from(double x, double y) {
        double magnitude = Math.sqrt(x*x + y*y);
        double unitX = x / magnitude;
        double unitY = y / magnitude;
        double angle = computeAngle(unitX, unitY);
        return new UnitVector(unitX, unitY, angle);
    }

    public static UnitVector from(Point2D vector) {
        return from(vector.getX(), vector.getY());
    }

    public static UnitVector from(Point2D from, Point2D to) {
        return from(to.getX() - from.getX(), to.getY() - from.getY());
    }

    public static UnitVector from(double angle) {
        return new UnitVector(Math.cos(angle), Math.sin(angle), angle);
    }

    public UnitVector rotate(double delta) {
        return from(addAngle(angle, delta));
    }

    public UnitVector leftNormal() {
        return new UnitVector(-getY(), getX(), addAngle(angle, Math.PI / 2));
    }

    public UnitVector rightNormal() {
        return new UnitVector(getY(), -getX(), addAngle(angle, - Math.PI / 2));
    }

    public UnitVector inverse() {
        return new UnitVector(-getX(), -getY(), addAngle(angle, Math.PI));
    }

    private UnitVector(double unitX, double unitY, double angle) {
        super(unitX, unitY);
        this.angle = angle;
    }

    private static double computeAngle(double unitX, double unitY) {
        double angle = Math.acos(unitX);
        if (unitY < 0) {
            angle = 2*Math.PI - angle;
        }
        return angle;
    }

    private static double addAngle(double angle, double delta) {
        double result = angle + delta;
        while (result < 0) {
            result += 2*Math.PI;
        }
        while (result >= 2*Math.PI) {
            result -= 2*Math.PI;
        }
        return result;
    }
}
