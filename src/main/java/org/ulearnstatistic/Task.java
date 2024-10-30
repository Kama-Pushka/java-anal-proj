package org.ulearnstatistic;

import java.util.HashMap;
import java.util.Map;

public abstract class Task {
    protected final int id;
    protected final String name;
    protected final Map<Integer, Integer> trainingPoints = new HashMap<>();

    public Task(String name) {
        this.id = hashCode();
        this.name = name;
    }
}
