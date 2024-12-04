package org.ulearnstatistic.model;

import java.util.HashMap;
import java.util.Map;

public abstract class Task {
    protected final int id;
    protected final String name;
    protected final Map<Integer, Integer> points = new HashMap<>();

    public Task(String name) {
        this.id = hashCode();
        this.name = name;
    }

    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public int getPoint(int studentId) {
        return points.get(studentId);
    }
    public Map<Integer, Integer> getPoints() {
        return points;
    }

    public void addPoint(int studentId, int trainingPoint) {
        points.put(studentId, trainingPoint);
    }

    @Override
    public String toString() {
        return "Task [name=" + name + ", points_count=" + points.size() + ", points=" + points + "]"; // id=" + id + ",
    }
}
