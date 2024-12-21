package org.ulearnstatistic.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Module {
    private final int id;
    private final String name;
    private MaxPointModuleStatistic maxPoints;
    private final Map<Integer, ModuleStudentStatistic> statistics = new HashMap<>(); // TODO переделать?
    private final ArrayList<Training> trainings = new ArrayList<>();
    private final ArrayList<Practice> practices =  new ArrayList<>();
    private final ArrayList<ControlQuestion> cq =  new ArrayList<>();

    public Module(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public void setMaxPoints(MaxPointModuleStatistic maxPoints) {
        this.maxPoints = maxPoints;
    }
    public void addStatistic(int studentId, ModuleStudentStatistic statistic) {
        statistics.put(studentId, statistic);
    }
    public void addTask(Task task) {
        if (task instanceof Training) {
            trainings.add((Training)task);
        } else if (task instanceof Practice) {
            practices.add((Practice)task);
        } else if (task instanceof ControlQuestion) {
            cq.add((ControlQuestion)task);
        }
    }

    public int getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public MaxPointModuleStatistic getMaxPoints() {
        return maxPoints;
    }
    public ModuleStudentStatistic getStatistic(int id) {
        return statistics.get(id);
    }
    public Map<Integer, ModuleStudentStatistic> getStatistics() { return statistics; }
    public ArrayList<Training> getTrainings() { return trainings; }
    public ArrayList<Practice> getPractices() { return practices; }
    public ArrayList<ControlQuestion> getCq() { return cq; }
    public Training getTraining(String name) {
        for (var training : trainings) {
            if (Objects.equals(training.getName(), name)) {
                return training;
            }
        }
        return null;
    }
    public Practice getPractice(String name) {
        for (var practice : practices) {
            if (Objects.equals(practice.getName(), name)) {
                return practice;
            }
        }
        return null;
    }
    public ControlQuestion getControlQuestions(String name) {
        for (var question : cq) {
            if (Objects.equals(question.getName(), name)) {
                return question;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        //return String.format("Module: %s; statistics: %d; trainings: %d; practices: %d; cq: %d.",
        //        name, statistics.size(), trainings.size(), practices.size(), cq.size());
        return getFullInfo();
    }
    public String getFullInfo() {
        var builder = new StringBuilder();
        builder.append("Module: ").append(name).append("\n");
        builder.append("\tStatistics: %d\n".formatted(statistics.size()));
        builder.append("\tTrainings: %d\n".formatted(trainings.size()));
        for (var training : trainings) {
            builder.append("\t\t").append(training).append("\n");
        }
        builder.append("\tPractices: %d\n".formatted(practices.size()));
        for (var practice : practices) {
            builder.append("\t\t").append(practice).append("\n");
        }
        builder.append("\tCQ: %d\n".formatted(cq.size()));
        for (var question : cq) {
            builder.append("\t\t").append(question).append("\n");
        }
        return builder.toString();
    }
}
