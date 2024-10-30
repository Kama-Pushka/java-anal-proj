package org.ulearnstatistic;

public class ModuleStudentStatistic {
    private final int moduleId;
    private final int studentId;
    private int trainingPoint;
    private int practicePoint;
    private int seminarPoint;
    private int activityPoint;
    private int cqPoint;

    ModuleStudentStatistic(int moduleId, int studentId) {
        this.moduleId = moduleId;
        this.studentId = studentId;
    }
}
