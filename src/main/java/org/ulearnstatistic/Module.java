package org.ulearnstatistic;

import java.util.ArrayList;

public class Module {
    private final int id;
    private final String name;
    private ArrayList<ModuleStudentStatistic> statistics = new ArrayList<>();
    private ArrayList<Training> trainings = new ArrayList<>();
    private ArrayList<Practice> practices =  new ArrayList<>();
    private ArrayList<ControlQuestion> cq =  new ArrayList<>();

    public Module(String name) {
        this.name = name;
        this.id = hashCode();
    }
}
