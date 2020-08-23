package com.gcodebuilder.changelog;

public interface Snapshot<T> {
    T restore();
}
