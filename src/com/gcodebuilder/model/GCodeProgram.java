package com.gcodebuilder.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GCodeProgram {
    private final GCodeLine[] lines;

    public GCodeProgram(GCodeLine... lines) {
        this.lines = Arrays.copyOf(lines, lines.length);
    }

    public GCodeProgram(Collection<GCodeLine> lines) {
        this.lines = lines.toArray(GCodeLine[]::new);
    }

    public List<GCodeLine> getLines() {
        return List.of(lines);
    }

    public void print(PrintStream out) {
        for (GCodeLine line : lines) {
            out.println(line.toString());
        }
    }

    public void print(PrintWriter out) {
        for (GCodeLine line : lines) {
            out.println(line.toString());
        }
    }

    public static GCodeProgram load(InputStream in) throws IOException {
        throw new UnsupportedOperationException("load gcode not supported");
    }

    public void save(OutputStream out) throws IOException {
        print(new PrintStream(out));
    }
}
