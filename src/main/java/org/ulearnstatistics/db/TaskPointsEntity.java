package org.ulearnstatistics.db;

public class TaskPointsEntity {
    public int taskId;
    public int studentId;
    public int points;

    public TaskPointsEntity() {}

    public TaskPointsEntity(int taskId, int studentId, int points) {
        this.taskId = taskId;
        this.studentId = studentId;
        this.points = points;
    }
}
