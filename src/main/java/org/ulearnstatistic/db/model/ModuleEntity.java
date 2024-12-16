package org.ulearnstatistic.db.model;

import org.ulearnstatistic.model.Module;

public class ModuleEntity {
    public int id;
    public String name;
    //public Map<Integer, ModuleStudentStatistic> statistics = new HashMap<>();
    //public ArrayList<Training> trainings = new ArrayList<>();
    //public ArrayList<Practice> practices =  new ArrayList<>();
    //public ArrayList<ControlQuestion> cq =  new ArrayList<>();

    public ModuleEntity() {}

    public ModuleEntity(Module module) {
        id = module.getId();
        name = module.getName();
    }
}
