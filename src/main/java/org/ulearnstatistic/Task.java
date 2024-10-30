package org.ulearnstatistic;

import java.util.HashMap;
import java.util.Map;

public abstract class Task {
    protected final int id;
    protected final String name;
    protected final Map<String, Integer> points = new HashMap<>();

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

    public void addPoint(String studentName, int trainingPoint) {
        points.put(studentName, trainingPoint);
    }

    @Override
    public String toString() {
        return "Task [name=" + name + ", points_count=" + points.size() + ", points=" + points + "]"; // id=" + id + ",
    }
}
