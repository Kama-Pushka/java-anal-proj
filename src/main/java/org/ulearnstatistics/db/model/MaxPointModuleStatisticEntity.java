package org.ulearnstatistics.db.model;

import org.ulearnstatistics.model.MaxPointModuleStatistic;

public class MaxPointModuleStatisticEntity {
    public int moduleId;
    public int trainingPoint;
    public int practicePoint;
    public int seminarPoint;
    public int activityPoint;
    public int cqPoint;

    public MaxPointModuleStatisticEntity() {}

    public MaxPointModuleStatisticEntity(MaxPointModuleStatistic m) {
        moduleId = m.getModuleId();
        trainingPoint = m.getTrainingPoint();
        practicePoint = m.getPracticePoint();
        seminarPoint = m.getSeminarPoint();
        activityPoint = m.getActivityPoint();
        cqPoint = m.getCQPoint();
    }
}
