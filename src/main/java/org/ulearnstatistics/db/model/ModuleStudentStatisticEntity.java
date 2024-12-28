package org.ulearnstatistics.db.model;

import org.ulearnstatistics.model.ModuleStudentStatistic;

public class ModuleStudentStatisticEntity {
    public int moduleId;
    public int studentId;
    public int trainingPoint;
    public int practicePoint;
    public int seminarPoint;
    public int activityPoint;
    public int cqPoint;

    public ModuleStudentStatisticEntity() {}

    public ModuleStudentStatisticEntity(ModuleStudentStatistic m) {
        moduleId = m.getModuleId();
        studentId = m.getStudentId();
        trainingPoint = m.getTrainingPoint();
        practicePoint = m.getPracticePoint();
        seminarPoint = m.getSeminarPoint();
        activityPoint = m.getActivityPoint();
        cqPoint = m.getCQPoint();
    }
}
