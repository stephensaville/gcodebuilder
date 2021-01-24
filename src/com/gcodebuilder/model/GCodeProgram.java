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
