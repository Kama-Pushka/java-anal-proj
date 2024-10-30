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

    public void setPoint(String name, int points) {
        switch (name) {
            case "training" -> trainingPoint = points;
            case "practice" -> practicePoint = points;
            case "cq" -> cqPoint = points;
            case "activity" -> activityPoint = points;
            case "simenar" -> seminarPoint = points;
        }
    }

    public int getTrainingPoint() {
        return trainingPoint;
    }
    public int getPracticePoint() {
        return practicePoint;
    }
    public int getSeminarPoint() {
        return seminarPoint;
    }
    public int getActivityPoint() {
        return activityPoint;
    }
    public int getCQPoint() {
        return cqPoint;
    }
}
