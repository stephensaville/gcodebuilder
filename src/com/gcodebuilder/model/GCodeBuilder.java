package com.gcodebuilder.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class GCodeBuilder {
    @Getter
    private MotionMode motionMode;

    @Getter
    private UnitMode unitMode;

    @Getter
    private FeedRateMode feedRateMode;

    @Getter
    private DistanceMode distanceMode;

    @Getter
    private ArcDistanceMode arcDistanceMode;

    private List<GCodeLine> lines = new ArrayList<>();
    private List<GCodeWord> currentLine = new ArrayList<>();

    public GCodeBuilder add(GCodeWord word) {
        currentLine.add(word);
        return this;
    }

    public GCodeBuilder endLine() {
        if (!currentLine.isEmpty()) {
            lines.add(new GCodeLine(currentLine));
        }
        currentLine.clear();
        return this;
    }

    public GCodeBuilder emptyLine() {
        endLine();
        lines.add(new GCodeLine());
        return this;
    }

    public GCodeProgram build() {
        endLine();
        return new GCodeProgram(lines);
    }

    public GCodeBuilder comment(String text) {
        add(new GCodeComment(text));
        if (currentLine.size() == 1) {
            endLine();
        }
        return this;
    }

    private <T extends GCodeWord> T emitOnModeChange(T currentMode, T newMode) {
        if (newMode != currentMode) {
            add(newMode);
        }
        return newMode;
    }

    public GCodeBuilder motionMode(MotionMode motionMode) {
        this.motionMode = emitOnModeChange(this.motionMode, motionMode);
        return this;
    }

    public GCodeBuilder unitMode(UnitMode unitMode) {
        this.unitMode = emitOnModeChange(this.unitMode, unitMode);
        return this;
    }

    public GCodeBuilder feedRateMode(FeedRateMode feedRateMode) {
        this.feedRateMode = emitOnModeChange(this.feedRateMode, feedRateMode);
        return this;
    }

    public GCodeBuilder distanceMode(DistanceMode distanceMode) {
        this.distanceMode = emitOnModeChange(this.distanceMode, distanceMode);
        return this;
    }

    public GCodeBuilder arcDistanceMode(ArcDistanceMode arcDistanceMode) {
        this.arcDistanceMode = emitOnModeChange(this.arcDistanceMode, arcDistanceMode);
        return this;
    }

    public GCodeBuilder feedRate(int feedRate) {
        return add(new FeedRate(feedRate));
    }

    public GCodeBuilder X(double value) {
        return add(GCodeParam.X(value));
    }

    public GCodeBuilder Y(double value) {
        return add(GCodeParam.Y(value));
    }

    public GCodeBuilder Z(double value) {
        return add(GCodeParam.Z(value));
    }

    public GCodeBuilder XY(double x, double y) {
        return X(x).Y(y);
    }

    public GCodeBuilder XYZ(double x, double y, double z) {
        return X(x).Y(y).Z(z);
    }

    public GCodeBuilder I(double value) {
        return add(GCodeParam.I(value));
    }

    public GCodeBuilder J(double value) {
        return add(GCodeParam.J(value));
    }

    public GCodeBuilder K(double value) {
        return add(GCodeParam.K(value));
    }

    public GCodeBuilder IJ(double i, double j) {
        return I(i).J(j);
    }

    public GCodeBuilder IJK(double i, double j, double k) {
        return I(i).J(j).K(k);
    }
}
