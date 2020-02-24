package com.gcodebuilder.model;

import com.sun.management.GcInfo;
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
}
