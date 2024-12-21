package org.ulearnstatistic;

import java.io.*;
import java.util.*;

import com.google.gson.Gson;
import com.vk.api.sdk.objects.users.OccupationType;
import com.vk.api.sdk.objects.users.UserFull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import com.vk.api.sdk.objects.users.responses.SearchResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.ulearnstatistic.model.Sex;
import org.ulearnstatistic.model.Student;
import org.ulearnstatistic.model.University;

import org.apache.commons.lang3.tuple.Triple;
import org.ulearnstatistic.db.DBRepository;
import org.ulearnstatistic.db.TaskPointsEntity;
import org.ulearnstatistic.db.model.ModuleEntity;
import org.ulearnstatistic.db.model.ModuleStudentStatisticEntity;
import org.ulearnstatistic.db.model.StudentEntity;
import org.ulearnstatistic.db.model.TaskEntity;
import org.ulearnstatistic.model.Module;

import org.ulearnstatistic.parser.CSVUlearnReader;
import org.ulearnstatistic.vkApi.VkRepository;
import org.ulearnstatistic.vkApi.VkRequest;

public class Main {

    public static void main(String[] args) {
        try {
            var ulearn = new CSVUlearnReader();
            ulearn.read("data/java-rtf.csv");
            DBRepository.UpdateURL("jdbc:sqlite:data/java-rtf.db");

            var moduleList = ulearn.getModules();
            var students = ulearn.getStudents();

//            var groups = new long[] { 6214974, 195681601, 22941070 };
//            var studentDct = getStudentDataFromVk(students, groups);
//            //serializeStudentData(studentDct,"data\\student_info.txt");
            var newStudentDct = deserializeStudentData("data\\student_info.txt");
            setStudentData(newStudentDct,students);

            DBRepository.connect();

            // кладем в бд и получаем из нее информацию о студентах
            var studentEntities = new ArrayList<StudentEntity>();
            for (var student : students) {
                studentEntities.add(new StudentEntity(student));
            }
            createTable(StudentEntity.class, new String[] {"id"}, null);
            saveIntoTable(StudentEntity.class, studentEntities);
            var list = getAllDataFromTable(StudentEntity.class);

            // кладем в бд и получаем из нее информацию о модулях
            var modules = new ArrayList<ModuleEntity>();
            var moduleStudentStatistic = new ArrayList<ModuleStudentStatisticEntity>();
            var tasks = new ArrayList<TaskEntity>();
            var taskPointsEntity = new ArrayList<TaskPointsEntity>();
            parseModelToDataModel(moduleList, modules, moduleStudentStatistic, tasks, taskPointsEntity);

            createTable(ModuleEntity.class, new String[] {"id"}, null);
            saveIntoTable(ModuleEntity.class, modules);
            var list1 = getAllDataFromTable(ModuleEntity.class);

            var triple = new Triple[] {
                    Triple.of("moduleId", "ModuleEntity", "id"),
                    Triple.of("studentId", "StudentEntity", "id")
            };
            createTable(ModuleStudentStatisticEntity.class, new String[] {"moduleId, studentId"}, triple);
            saveIntoTable(ModuleStudentStatisticEntity.class, moduleStudentStatistic);
            var list2 = getAllDataFromTable(ModuleStudentStatisticEntity.class);

            createTable(TaskEntity.class, new String[] {"id"}, null);
            saveIntoTable(TaskEntity.class, tasks);
            var list3 = getAllDataFromTable(ModuleStudentStatisticEntity.class);

            triple = new Triple[] {
                    Triple.of("taskId", "TaskEntity", "id"),
                    Triple.of("studentId", "StudentEntity", "id")
            };
            createTable(TaskPointsEntity.class, new String[] {"taskId,  studentId"}, triple);
            saveIntoTable(TaskPointsEntity.class, taskPointsEntity);
            var list4 = getAllDataFromTable(TaskPointsEntity.class);

            ulearn.write("data\\report.txt", true, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static HashMap<String, SearchResponse> getStudentDataFromVk(List<Student> students, long[] groups) {
        var vk = new VkRepository();
        var studentDct = VkRequest.searchStudentsByGroups(vk,students,groups);

        for (var key : studentDct.keySet()) {
            System.out.println(key + "\t" + studentDct.get(key));
        }

        return studentDct;
    }

    private static void parseModelToDataModel(ArrayList<Module> moduleList,
                                              ArrayList<ModuleEntity> modules,
                                              ArrayList<ModuleStudentStatisticEntity> moduleStudentStatistics,
                                              ArrayList<TaskEntity> tasks,
                                              ArrayList<TaskPointsEntity> taskPointsEntities) {
        for (var module : moduleList) {
            modules.add(new ModuleEntity(module));
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

    private static void createTable(Class<?> clazz, String[] keys, Triple<String, String, String>[] foreignKeys) {
        var sqlQuery = DBRepository.getCreateTableQuery(clazz, keys, foreignKeys);
        DBRepository.createTable(sqlQuery);
    }

    private static void saveIntoTable(Class<?> clazz, List<?> objects) {
        var sql = DBRepository.getSaveIntoTableQuery(clazz);
        DBRepository.saveIntoTable(sql.getKey(), objects, sql.getValue());
    }

    private static List<?> getAllDataFromTable(Class<?> clazz) {
        var sql = DBRepository.getDataFromTableQuery(clazz);
        return DBRepository.getDataFromTable(sql.getKey(), clazz, sql.getValue());
    }

    private static void setStudentData(HashMap<String, SearchResponse> studentDct, ArrayList<Student> students) {
        var studentData = getWeightListsForStudentDatas(studentDct);
        for (var student : students) {
            var temp = studentData.get(student.getName());
            if (!temp.isEmpty()) {
                var data = temp.getFirst().getValue();
                if (data.getUniversity() != null && data.getUniversityName() != null) {
                    student.setUniversity(new University(data.getUniversity(), data.getUniversityName()));
                }

                if (data.getHomeTown() != null) {
                    student.setCity(data.getHomeTown());
                } else if (data.getCity() != null) {
                    student.setCity(data.getCity().getTitle());
                } else if (data.getUniversity() != null && data.getUniversity() == 422) {
                    student.setCity("Екатеринбург");
                }

                student.setSex(Sex.values()[data.getSex().getValue()]); // TODO ??

                if (data.getBdate() != null) { // TODO
                    var date = data.getBdate().split("\\.");
                    var day = date.length > 0 ? Integer.parseInt(date[0]) : 0;
                    var month = date.length > 1 ? Integer.parseInt(date[1]) : 0;
                    var year = date.length > 2 ? Integer.parseInt(date[2]) : 0;
                    student.setDateOfBirth(day, month, year);
                }

                student.setName(data.getFirstName()); // TODO ??
                student.setSurname(data.getLastName());
            }
        }
    }

    private static HashMap<String, ArrayList<Pair<Integer, UserFull>>> getWeightListsForStudentDatas(HashMap<String, SearchResponse> studentDct) {
        var result = new HashMap<String, ArrayList<Pair<Integer, UserFull>>>();
        for (var key : studentDct.keySet()) {
            var weightList = new ArrayList<Pair<Integer, UserFull>>();
            for (var data : studentDct.get(key).getItems()) {
                weightList.add(setWeightList(data));
            }
            weightList.sort(Comparator.comparing(Pair::getKey)); // TODO ?
            result.put(key, weightList);
        }
        return result;
    }

    private static Pair<Integer, UserFull> setWeightList(UserFull userData) {
        var isUrfuStudent = false;
        var weight = 0;
        if (userData.getUniversities() != null) {
            for (var univ : userData.getUniversities()) {
                if (univ.getId() == 477) { // TODO
                    userData.setUniversity(477);
                    userData.setUniversityName(univ.getName());
                    isUrfuStudent = true;
                }

                if (isUrfuStudent && univ.getFaculty() != null && univ.getFaculty() != 20422 && univ.getFaculty() != 20422) { // TODO!!
                    weight--;
                } else if (isUrfuStudent && univ.getFaculty() != null && (univ.getFaculty() == 20422 || univ.getFaculty() == 20422)) {
                    weight++;
                }
                if (univ.getEducationStatusId() != null && (univ.getEducationStatusId() != 0 && univ.getEducationStatusId() != 1) // TODO!!
                        || univ.getGraduation() != null && univ.getGraduation() <= 2018) {
                    weight -= 2;
                }
                if (univ.getChair() != null && (univ.getChair() == 2101361 || univ.getChair() == 2101125 || univ.getChair() == 2101127)) {
                    weight++;
                }
            }
        }
        if (!isUrfuStudent) {
            var occup = userData.getOccupation();
            if (occup != null && occup.getType() == OccupationType.UNIVERSITY) {
                if (occup.getId() != null && occup.getId() == 477) { // TODO ???
                    userData.setUniversity(477);
                    userData.setUniversityName(occup.getName());
                    isUrfuStudent = true;
                }

                if (occup.getGraduateYear() != null && occup.getGraduateYear() <= 2018) {
                    weight -= 2;
                }
            }
        }

        return Pair.of(weight, userData);
    }

    // временно сохраняем полученную информацию в файле
    private static void serializeStudentData(HashMap<String,SearchResponse> studentDct, String path) throws IOException {
        var gson = new Gson();
        var write = new StringBuilder();
        for (var key : studentDct.keySet()) {
            var peoples = studentDct.get(key);
            write.append(key).append("\t");
            write.append(gson.toJson(peoples));
            write.append("\n");
        }
        try (var report = new FileWriter(path)) {
            report.write(write.toString());
            System.out.printf("INFO written to file %s.%n", path);
        }
    }

    private static HashMap<String,SearchResponse> deserializeStudentData(String path) throws IOException {
        var sc = new Scanner(new File(path));
        var result = new HashMap<String,SearchResponse>();
        var gson = new Gson();
        while (sc.hasNextLine()) {
            var line = sc.nextLine();
            var data = line.split("\t");
            var searchResponse = gson.fromJson(data[1], SearchResponse.class);
            result.put(data[0], searchResponse);
        }
        return result;
    }
}
