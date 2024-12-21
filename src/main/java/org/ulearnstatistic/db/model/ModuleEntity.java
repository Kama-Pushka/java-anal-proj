package org.ulearnstatistic.db.model;

import org.ulearnstatistic.model.Module;

public class ModuleEntity {
    public int id;
    public String name;

    public ModuleEntity() {}

    public ModuleEntity(Module module) {
        id = module.getId();
        name = module.getName();
    }
}
