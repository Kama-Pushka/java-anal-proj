package org.ulearnstatistic.db;

import org.apache.commons.lang3.tuple.Triple;
import org.ulearnstatistic.db.model.MaxPointModuleStatisticEntity;
import org.ulearnstatistic.db.model.ModuleEntity;
import org.ulearnstatistic.db.model.ModuleStudentStatisticEntity;
import org.ulearnstatistic.db.model.TaskEntity;
import org.ulearnstatistic.model.Module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BDRepository {

    public static void parseModelToDataModel(List<Module> moduleList,
                                             List<MaxPointModuleStatisticEntity> maxPointModule,
                                             List<ModuleEntity> modules,
                                             List<ModuleStudentStatisticEntity> moduleStudentStatistics,
                                             List<TaskEntity> tasks,
                                             List<TaskPointsEntity> taskPointsEntities) {
        for (var module : moduleList) {
            modules.add(new ModuleEntity(module));
            maxPointModule.add(new MaxPointModuleStatisticEntity(module.getMaxPoints()));
            var map = module.getStatistics();
            for (var values : map.values()) {
                moduleStudentStatistics.add(new ModuleStudentStatisticEntity(values));
            }
            for (var values : module.getTrainings()) {
                var task = new TaskEntity(values, module.getId());
                tasks.add(task);
                taskPointsEntities.addAll(task.getSubtasks(values));
            }
            for (var values : module.getPractices()) {
                var task = new TaskEntity(values, module.getId());
                tasks.add(task);
                taskPointsEntities.addAll(task.getSubtasks(values));
            }
            for (var values : module.getCq()) {
                var task = new TaskEntity(values, module.getId());
                tasks.add(task);
                taskPointsEntities.addAll(task.getSubtasks(values));
            }
        }
    }

    public static void createTable(Class<?> clazz, String[] keys, Triple<String, String, String>[] foreignKeys) {
        var sqlQuery = DBService.getCreateTableQuery(clazz, keys, foreignKeys);
        DBService.createTable(sqlQuery);
    }

    public static void saveIntoTable(Class<?> clazz, List<?> objects) {
        var sql = DBService.getSaveIntoTableQuery(clazz);
        DBService.saveIntoTable(sql.getKey(), objects, sql.getValue());
    }

    public static List<?> getAllDataFromTable(Class<?> clazz) {
        var sql = DBService.getDataFromTableQuery(clazz);
        return DBService.getDataFromTable(sql.getKey(), clazz, sql.getValue());
    }

    public static HashMap<String, Double> getAVGForField(String moduleName, String pointName, boolean max) {
        var sql = max
                ? getAVGForFieldMaxQuery(moduleName, pointName)
                : getAVGForFieldQuery(moduleName, pointName);
        var result = DBService.getDataFromTable(sql);
//        var map = new HashMap<String, Double>();
//        for (var value : result) {
//            map.put(result.getString(1), result.getDouble(2));
//        }
//        return map;
        return DBService.getAVGForField(sql);
    }

    private static String getAVGForFieldQuery(String moduleName, String pointName) {
        if (moduleName.equals("All")) {
            return "SELECT me.name, AVG(%s) AS %s\n".formatted(pointName, pointName)
                    + "FROM ModuleStudentStatisticEntity JOIN ModuleEntity me\n"
                    + "ON moduleId = me.id\n"
                    + "GROUP BY moduleId";
        } else {
            return "SELECT te.name, AVG(points) AS points\n"
                    + "FROM TaskPointsEntity tpe\n"
                    + "JOIN TaskEntity te ON taskId = te.id\n"
                    + "JOIN ModuleEntity me ON te.moduleId = me.id\n"
                    + "WHERE me.name = \"%s\"\n".formatted(moduleName) // TODO id?
                    + "GROUP BY te.id";
        }
    }
    private static String getAVGForFieldMaxQuery(String moduleName, String pointName) {
        if (moduleName.equals("All")) {
            return "SELECT me.name, AVG(mse.%s)/mxmse.%s AS %s\n".formatted(pointName, pointName, pointName)
                    + "FROM ModuleStudentStatisticEntity mse\n"
                    + "JOIN ModuleEntity me ON mse.moduleId = me.id\n"
                    + "JOIN MaxPointModuleStatisticEntity mxmse ON mse.moduleId = mxmse.moduleId\n"
                    + "GROUP BY mse.moduleId";
        } else {
            return "SELECT te.name, AVG(points)/te.maxPoint AS points\n"
                    + "FROM TaskPointsEntity tpe\n"
                    + "JOIN TaskEntity te ON taskId = te.id\n"
                    + "JOIN ModuleEntity me ON te.moduleId = me.id\n"
                    + "WHERE me.name = \"%s\"\n".formatted(moduleName) // TODO id?
                    + "GROUP BY te.id";
        }
    }

    public static Map<String, Map<String, Double>> getAVGForFieldForLineChart(String module, String field, String pointName, boolean max, boolean filter) {
        var sql = max
                ? getAVGForFieldForLineChartMaxQuery(module, field, pointName, filter)
                : getAVGForFieldForLineChartQuery(module, field, pointName, filter);
        var result = DBService.getDataFromTable(sql);
//        var map = new HashMap<String, Map<String, Double>>();     TODO !!!!
//        for (var value : result) {
//            var moduleName = result.getString(1);
//            var fieldName = result.getString(2);
//            var point = result.getDouble(3);
//            if (map.get(fieldName) == null) {
//                map.put(fieldName, new HashMap<>());
//            }
//            map.get(fieldName).put(moduleName, point);
//        }
//        return map;
        return DBService.getAVGForFieldForLineChart(sql);
    }

    private static String getAVGForFieldForLineChartMaxQuery(String moduleName, String field, String pointName, boolean filter) {
        if (moduleName.equals("All")) {
            return "SELECT me.name, se.\"%s\", AVG(mse.%s)/mxmse.%s AS %s\n".formatted(field, pointName, pointName, pointName)
                    + "FROM ModuleStudentStatisticEntity mse\n"
                    + "JOIN StudentEntity se ON mse.studentId = se.id\n"
                    + "JOIN ModuleEntity me ON mse.moduleId = me.id\n"
                    + "JOIN MaxPointModuleStatisticEntity mxmse ON mse.moduleId = mxmse.moduleId\n"
                    + "GROUP BY me.id, se.\"%s\"".formatted(field)
                    + (filter ? "HAVING COUNT(se.\"%s\") >= 3".formatted(field) : "");
        } else {
            return "SELECT te.name, se.\"%s\", AVG(points)/te.maxPoint AS points\n".formatted(field)
                    + "FROM TaskPointsEntity\n"
                    + "JOIN StudentEntity se ON studentId = se.id\n"
                    + "JOIN TaskEntity te ON taskId = te.id\n"
                    + "JOIN ModuleEntity me ON te.moduleId = me.id\n"
                    + "WHERE me.name = '%s'\n".formatted(moduleName) // TODO id?
                    + "GROUP BY te.id, se.\"%s\"".formatted(field)
                    + (filter ? "HAVING COUNT(se.\"%s\") >= 3".formatted(field) : "");
        }
    }

    private static String getAVGForFieldForLineChartQuery(String moduleName, String field, String pointName, boolean filter) {
        if (moduleName.equals("All")) {
            return "SELECT me.name, se.\"%s\", AVG(%s) AS %s\n".formatted(field, pointName, pointName)
                    + "FROM ModuleStudentStatisticEntity\n"
                    + "JOIN StudentEntity se ON studentId = se.id\n"
                    + "JOIN ModuleEntity me ON moduleId = me.id\n"
                    + "GROUP BY me.id, se.\"%s\"".formatted(field)
                    + (filter ? "HAVING COUNT(se.\"%s\") >= 3".formatted(field) : "");
        } else {
            return "SELECT te.name, se.\"%s\", AVG(points) AS points\n".formatted(field)
                    + "FROM TaskPointsEntity\n"
                    + "JOIN StudentEntity se ON studentId = se.id\n"
                    + "JOIN TaskEntity te ON taskId = te.id\n"
                    + "JOIN ModuleEntity me ON te.moduleId = me.id\n"
                    + "WHERE me.name = '%s'\n".formatted(moduleName) // TODO id?
                    + "GROUP BY te.id, se.\"%s\"".formatted(field)
                    + (filter ? "HAVING COUNT(se.\"%s\") >= 3".formatted(field) : "");
        }
    }
}
