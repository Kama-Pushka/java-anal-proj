package org.ulearnstatistics.db;

import org.apache.commons.lang3.tuple.Triple;
import org.ulearnstatistics.db.model.*;
import org.ulearnstatistics.model.Module;

import java.util.*;

public class BDRepository {

    /**
     * Перевод domain-моделей в data-модели базы данных.
     */
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

    /**
     * Создание таблицы по структуре переданного data-model класса.
     * @param clazz Data-Model класс
     * @param keys Ключевые столбцы - названия полей класса
     * @param foreignKeys Внешние ключи - название полей класса, таблицы, с которой создается связь
     *                    и название колонок той таблицы, на которые создаются ссылки
     */
    public static void createTable(Class<?> clazz, String[] keys, Triple<String, String, String>[] foreignKeys) {
        var sqlQuery = DBService.getCreateTableQuery(clazz, keys, foreignKeys);
        DBService.createTable(sqlQuery);
    }

    /**
     * Сохранить всю информацию о data-model классах в таблицы.
     * @param clazz Data-model класс, в таблицу которого будут добавлены данные
     * @param objects Объекты Data-model класса, данные о которых будут сохранены
     */
    public static void saveIntoTable(Class<?> clazz, List<?> objects) {
        var sql = DBService.getSaveIntoTableQuery(clazz);
        DBService.saveIntoTable(sql.getKey(), objects, sql.getValue());
    }

    /**
     * Получить все данные из таблицы data-model класса.
     * @param clazz Data-model класс, из таблицы которого берутся данные
     * @return Список data-model классов с записанными данными.
     */
    public static List<?> getAllDataFromTable(Class<?> clazz) {
        var sql = DBService.getDataFromTableQuery(clazz);
        return DBService.getDataFromTable(sql.getKey(), clazz, sql.getValue());
    }

    /**
     * Получить AVG значение баллов по колонке в определенном модуле.
     * @param moduleName Название модуля, из которого берется колонка
     *                   (если "All", то подсчет будет происходить для всех модулей, иначе по заданиям конкретного модуля,
     *                   а не по колонке)
     * @param pointName Название колонки (типа баллов), по которому будет браться AVG значение (например, trainingPoint)
     * @param isMax Определяет, будет ли нормализовано значение баллов в диапазон от 0 до 1.
     * @return HashMap, где в качестве ключа выступает название модулей (заданий модуля), а значением - среднее значение по колонке
     */
    public static HashMap<String, Double> getAVGForField(String moduleName, String pointName, boolean isMax) {
        var sql = isMax
                ? getAVGForFieldMaxQuery(moduleName, pointName)
                : getAVGForFieldQuery(moduleName, pointName);
        var result = DBService.getDataFromTable(sql);
        var map = new LinkedHashMap<String, Double>();
        for (var row : result) {
            var value = row.split("\\|\\|");
            map.put(value[0], Double.valueOf(value[1].equals("null") ? "0" : value[1]));
        }
        return map;
    }

    /**
     * Формирование запроса на получение AVG значения баллов по колонке в определенном модуле.
     * @param moduleName Название модуля, из которого берется колонка
     *                   (если "All", то подсчет будет происходить для всех модулей, иначе по заданиям конкретного модуля,
     *                   а не по колонке)
     * @param pointName Название колонки (типа баллов), по которому будет браться AVG значение (например, trainingPoint)
     * @return SQL-запрос
     */
    private static String getAVGForFieldQuery(String moduleName, String pointName) {
        if (moduleName.equals("All")) {
            return "SELECT me.name, AVG(%s) AS %s\n".formatted(pointName, pointName)
                    + "FROM ModuleStudentStatisticEntity JOIN ModuleEntity me\n"
                    + "ON moduleId = me.id\n"
                    + "GROUP BY moduleId\n"
                    + "ORDER BY me.id";
        } else {
            return "SELECT te.name, AVG(points) AS points\n"
                    + "FROM TaskPointsEntity tpe\n"
                    + "JOIN TaskEntity te ON taskId = te.id\n"
                    + "JOIN ModuleEntity me ON te.moduleId = me.id\n"
                    + "WHERE me.name = \"%s\"\n".formatted(moduleName) // TODO id?
                    + "GROUP BY te.id\n"
                    + "ORDER BY te.id";
        }
    }

    /**
     * Формирование запроса на получение AVG значения баллов в нормализованном виде (от 0 до 1) по колонке в определенном модуле.
     * @param moduleName Название модуля, из которого берется колонка
     *                   (если "All", то подсчет будет происходить для всех модулей, иначе по заданиям конкретного модуля,
     *                   а не по колонке)
     * @param pointName Название колонки (типа баллов), по которому будет браться AVG значение (например, trainingPoint)
     * @return SQL-запрос
     */
    private static String getAVGForFieldMaxQuery(String moduleName, String pointName) {
        if (moduleName.equals("All")) {
            return "SELECT me.name, AVG(mse.%s)/mxmse.%s AS %s\n".formatted(pointName, pointName, pointName)
                    + "FROM ModuleStudentStatisticEntity mse\n"
                    + "JOIN ModuleEntity me ON mse.moduleId = me.id\n"
                    + "JOIN MaxPointModuleStatisticEntity mxmse ON mse.moduleId = mxmse.moduleId\n"
                    + "GROUP BY mse.moduleId\n"
                    + "ORDER BY me.id";
        } else {
            return "SELECT te.name, AVG(points)/te.maxPoint AS points\n"
                    + "FROM TaskPointsEntity tpe\n"
                    + "JOIN TaskEntity te ON taskId = te.id\n"
                    + "JOIN ModuleEntity me ON te.moduleId = me.id\n"
                    + "WHERE me.name = \"%s\"\n".formatted(moduleName) // TODO id?
                    + "GROUP BY te.id\n"
                    + "ORDER BY te.id";
        }
    }

    /**
     * Получение AVG значения баллов по колонке в определенном модуле
     * относительно какого-либо свойства студента (пол, город и т.д).
     * @param module Название модуля, из которого берется колонка
     *               (если "All", то подсчет будет происходить для всех модулей, иначе по заданиям конкретного модуля,
     *               а не по колонке)
     * @param field Название колонки-свойства студента (пол, город и т.д)
     * @param pointName Название колонки (типа баллов), по которому будет браться AVG значение (например, trainingPoint)
     * @param isMax Определяет, будет ли нормализовано значение баллов в диапазон от 0 до 1.
     * @param filter Тип фильтрации
     * @param filterCriteria Значение для фильтрации
     * @return Map, где в качестве ключа выступает название модулей (заданий модуля),
     * а значением - Map, ключом которого является свойство студента, а значением - среднее значение по колонке
     */
    public static Map<String, Map<String, Double>> getAVGForFieldForLineChart(String module, String field, String pointName,
                                                                              boolean isMax, String filter, String filterCriteria) {
        var sql = isMax
                ? getAVGForFieldForLineChartMaxQuery(module, field, pointName, filter, filterCriteria)
                : getAVGForFieldForLineChartQuery(module, field, pointName, filter, filterCriteria);
        var result = DBService.getDataFromTable(sql);
        var map = new LinkedHashMap<String, Map<String, Double>>();
        for (var row : result) {
            var value = row.split("\\|\\|");
            var moduleName = value[0];
            var fieldName = value[1];
            var point = Double.valueOf(value[2].equals("null") ? "0" : value[2]);
            if (map.get(fieldName) == null) {
                map.put(fieldName, new LinkedHashMap<>());
            }
            map.get(fieldName).put(moduleName, point);
        }
        return map;
    }

    /**
     * Формирование запроса на получение AVG значения баллов по колонке в определенном модуле
     * относительно какого-либо свойства студента (пол, город и т.д).
     * @param moduleName Название модуля, из которого берется колонка
     *                   (если "All", то подсчет будет происходить для всех модулей, иначе по заданиям конкретного модуля,
     *                   а не по колонке)
     * @param field Название колонки-свойства студента (пол, город и т.д)
     * @param pointName Название колонки (типа баллов), по которому будет браться AVG значение (например, trainingPoint)
     * @param filter Тип фильтрации
     * @param filterCriteria Значение для фильтрации
     * @return SQL-запрос
     */
    private static String getAVGForFieldForLineChartQuery(String moduleName, String field, String pointName, String filter, String filterCriteria) {
        if (moduleName.equals("All")) {
            return "SELECT me.name, %s AVG(%s) AS %s\n".formatted(!field.isEmpty() ? "se.\"%s\", ".formatted(field) : "", pointName, pointName)
                    + "FROM ModuleStudentStatisticEntity\n"
                    + "JOIN StudentEntity se ON studentId = se.id\n"
                    + "JOIN ModuleEntity me ON moduleId = me.id\n"
                    + (filter.equals("student") ? "WHERE se.name = '%s'\n".formatted(filterCriteria) : "")
                    + "GROUP BY me.id %s\n".formatted(!field.isEmpty() ? ", se.\"%s\"".formatted(field) : "")
                    + (filter.equals("count") ? "HAVING COUNT(se.\"%s\") >= %s\n".formatted(field, filterCriteria) : "")
                    + "ORDER BY me.id";
        } else {
            return "SELECT te.name, %s AVG(points) AS points\n".formatted(!field.isEmpty() ? "se.\"%s\", ".formatted(field) : "")
                    + "FROM TaskPointsEntity\n"
                    + "JOIN StudentEntity se ON studentId = se.id\n"
                    + "JOIN TaskEntity te ON taskId = te.id\n"
                    + "JOIN ModuleEntity me ON te.moduleId = me.id\n"
                    + "WHERE me.name = '%s' %s\n".formatted(moduleName, filter.equals("student") ? "AND se.name = '%s'".formatted(filterCriteria) : "") // TODO id?
                    + "GROUP BY te.id %s\n".formatted(!field.isEmpty() ? ", se.\"%s\"".formatted(field) : "")
                    + (filter.equals("count") ? "HAVING COUNT(se.\"%s\") >= %s\n".formatted(field, filterCriteria) : "")
                    + "ORDER BY te.id";
        }
    }

    /**
     * Формирование запроса на получение AVG значения баллов в нормализованном виде (от 0 до 1) по колонке в определенном модуле
     * относительно какого-либо свойства студента (пол, город и т.д).
     * @param moduleName Название модуля, из которого берется колонка
     *                   (если "All", то подсчет будет происходить для всех модулей, иначе по заданиям конкретного модуля,
     *                   а не по колонке)
     * @param field Название колонки-свойства студента (пол, город и т.д)
     * @param pointName Название колонки (типа баллов), по которому будет браться AVG значение (например, trainingPoint)
     * @param filter Тип фильтрации
     * @param filterCriteria Значение для фильтрации
     * @return SQL-запрос
     */
    private static String getAVGForFieldForLineChartMaxQuery(String moduleName, String field, String pointName, String filter, String filterCriteria) {
        if (moduleName.equals("All")) {
            return "SELECT me.name, %s AVG(mse.%s)/mxmse.%s AS %s\n".formatted(!field.isEmpty() ? "se.\"%s\", ".formatted(field) : "", pointName, pointName, pointName)
                    + "FROM ModuleStudentStatisticEntity mse\n"
                    + "JOIN StudentEntity se ON mse.studentId = se.id\n"
                    + "JOIN ModuleEntity me ON mse.moduleId = me.id\n"
                    + "JOIN MaxPointModuleStatisticEntity mxmse ON mse.moduleId = mxmse.moduleId\n"
                    + (filter.equals("student") ? "WHERE se.name = '%s'\n".formatted(filterCriteria) : "")
                    + "GROUP BY me.id %s\n".formatted(!field.isEmpty() ? ", se.\"%s\"".formatted(field) : "")
                    + (filter.equals("count") ? "HAVING COUNT(se.\"%s\") >= %s\n".formatted(field, filterCriteria) : "")
                    + "ORDER BY me.id";
        } else {
            return "SELECT te.name, %s AVG(points)/te.maxPoint AS points\n".formatted(!field.isEmpty() ? "se.\"%s\", ".formatted(field) : "")
                    + "FROM TaskPointsEntity\n"
                    + "JOIN StudentEntity se ON studentId = se.id\n"
                    + "JOIN TaskEntity te ON taskId = te.id\n"
                    + "JOIN ModuleEntity me ON te.moduleId = me.id\n"
                    + "WHERE me.name = '%s' %s\n".formatted(moduleName, filter.equals("student") ? "AND se.name = '%s'".formatted(filterCriteria) : "") // TODO id?
                    + "GROUP BY te.id %s\n".formatted(!field.isEmpty() ? ", se.\"%s\"".formatted(field) : "")
                    + (filter.equals("count") ? "HAVING COUNT(se.\"%s\") >= %s\n".formatted(field, filterCriteria) : "")
                    + "ORDER BY te.id";
        }
    }
}
