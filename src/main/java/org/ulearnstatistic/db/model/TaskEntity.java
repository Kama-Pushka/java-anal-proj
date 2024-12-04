package org.ulearnstatistic.db.model;

import org.ulearnstatistic.db.TaskPointsEntity;
import org.ulearnstatistic.model.Task;

import java.util.ArrayList;

public class TaskEntity {
    public int id;
    public int moduleId;
    public String name;

    public TaskEntity() {}

    public TaskEntity(Task task, int moduleId) {
        id = task.getId();
        name = task.getName();
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

