package com.gcodebuilder.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GCodeLine {
    private final GCodeWord[] words;

    public GCodeLine(GCodeWord... words) {
        this.words = Arrays.copyOf(words, words.length);
    }

    public GCodeLine(Collection<GCodeWord> words) {
        this.words = words.toArray(GCodeWord[]::new);
    }

    public List<GCodeWord> getWords() {
        return List.of(words);
    }

    public String toString() {
        return String.join(" ", (Iterable<String>)Arrays.stream(words).map(GCodeWord::toGCode)::iterator);
    }
}
