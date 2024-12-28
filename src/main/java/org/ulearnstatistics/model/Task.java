package org.ulearnstatistics.model;

import java.util.HashMap;
import java.util.Map;

public abstract class Task {
    protected final int id;
    protected final String name;
    protected int maxPoint;
    protected final Map<Integer, Integer> points = new HashMap<>();

    public Task(String name, int id) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public int getMaxPoint() {
        return maxPoint;
    }
    public int getPoint(int studentId) {
        return points.get(studentId);
    }
    public Map<Integer, Integer> getPoints() {
        return points;
    }

    public void setMaxPoint(int maxPoint) {
        this.maxPoint = maxPoint;
    }
    public void addPoint(int studentId, int trainingPoint) {
        points.put(studentId, trainingPoint);
    }

    @Override
    public String toString() {
        return "Task [name=" + name + ", points_count=" + points.size() + ", points=" + points + "]"; // id=" + id + ",
    }
}
