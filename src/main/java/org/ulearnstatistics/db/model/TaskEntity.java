package org.ulearnstatistics.db.model;

import org.ulearnstatistics.model.Task;

import java.util.ArrayList;

public class TaskEntity {
    public int id;
    public int moduleId;
    public String name;
    public int maxPoint;
    //public Map<String, Integer> points = new HashMap<>();

    public TaskEntity() {}

    public TaskEntity(Task task, int moduleId) {
        id = task.getId();
        name = task.getName();
        maxPoint = task.getMaxPoint();
        this.moduleId = moduleId;
    }

    public ArrayList<TaskPointsEntity> getSubtasks(Task task) {
        var subtasks = new ArrayList<TaskPointsEntity>();
        var map = task.getPoints();
        for (var studentId : map.keySet()) {
            subtasks.add(new TaskPointsEntity(id, studentId, map.get(studentId)));
        }
        return subtasks;
    }
}

