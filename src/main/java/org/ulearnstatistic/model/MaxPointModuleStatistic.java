package org.ulearnstatistic.model;

public class MaxPointModuleStatistic {
    private final int moduleId;
    private int trainingPoint;
    private int practicePoint;
    private int seminarPoint;
    private int activityPoint;
    private int cqPoint;

    public MaxPointModuleStatistic(int moduleId) {
        this.moduleId = moduleId;
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

    public int getModuleId() {
        return moduleId;
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
