package com.gcodebuilder.geometry;

import com.gcodebuilder.app.tools.Tool;
import com.gcodebuilder.canvas.Drawable;

public abstract class Shape implements Drawable {
    public abstract Tool getEditingTool();
}
