package org.ulearnstatistic.parser;

import org.ulearnstatistic.model.*;
import org.ulearnstatistic.model.Module;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CSVUlearnReader {
    private ArrayList<Student> students;
    private ArrayList<Module> modules;

    public ArrayList<Student> getStudents() {
        return students;
    }
    public ArrayList<Module> getModules() {
        return modules;
    }

    public void read(String path) throws IOException {
        var scanner = new Scanner(new File(path));

        var modules = readModules(scanner.nextLine());
        var studentColsNum = modules.removeFirst().getNumCol();
        var titles = scanner.nextLine().split(";");
        var maxValues = scanner.nextLine().split(";"); // TODO использовать

        students = new ArrayList<Student>();
        var titleModulesData = Arrays.copyOfRange(titles,studentColsNum,titles.length);
        while (scanner.hasNextLine()) {
            var line = scanner.nextLine().split(";");
            var studentData = Arrays.copyOfRange(line,0,studentColsNum);
            var modulesData = Arrays.copyOfRange(line,studentColsNum,line.length);

            var student = readStudentData(titles,studentData);
            students.add(student);
            readModulesTasksData(student,titleModulesData,modulesData,modules);
        }

        this.modules = new ArrayList<Module>();
        for (var module : modules) {
            this.modules.add(module.getModule());
        }
    }

    private ArrayList<ModuleParser> readModules(String line) {
        var moduleNames = line.split(";", -1);
        moduleNames[0] = ""; // for UTF8-BOM
        var moduleName = "Students";
        var modules = new ArrayList<ModuleParser>();

        var lastModuleIndex = 0;
        for (var i = 0; i < moduleNames.length; i++) {
            if (!Objects.equals(moduleNames[i], "")) {
                modules.add(new ModuleParser(moduleName,i - lastModuleIndex));
                moduleName = moduleNames[i];
                lastModuleIndex = i;
            }

            if (i == moduleNames.length - 1) {
                modules.add(new ModuleParser(moduleName,i - lastModuleIndex + 1));
            }
        }
        return modules;
    }

    private Student readStudentData(String[] titles, String[] data) {
        var student = new Student();
        for (var i = 0; i < data.length; i++) {
            StudentParser.setStudentData(student, titles[i], data[i]);
        }
        return student;
    }

    private void readModulesTasksData(Student student, String[] titles, String[] attrs, ArrayList<ModuleParser> modules) {
        var m = new ArrayDeque<ModuleParser>(modules); // TODO
        var module = m.pop();

        var lastModuleIndex = 0;
        for (var i = 0; i < attrs.length; i++) {
            if (i - lastModuleIndex == module.getNumCol()) {
                module = m.pop();
                lastModuleIndex = i;
            }

            module.setStatistic(student.getId(), titles[i], attrs[i]); // общая статистика студента по типу задания (если это оно)
            module.setTask(student.getId(), titles[i], attrs[i]); // статистика студента по конкретному заданию (если это оно)
        }
    }

    public void write(String path, boolean writeStudents, boolean writeModules) throws IOException {
        var write = new StringBuilder();
        var stud = students.toArray();
        var mod = modules.toArray();
        if (writeStudents) {
            //write.append("Students: %d\n%s".formatted(stud.length, Arrays.toString(stud)));
            for (var student : students) {
                write.append("%s %s:\n\tПол: %s\n\tВозраст: %s (%s)\n\tГруппа: %s\n\tСтрана: %s\n\tГород: %s\n\tУниверситет: %s\n"
                        .formatted(student.getName(), student.getSurname() != null ? student.getSurname() : "",
                                student.getSex() != null ? student.getSex().name() : 0,
                                student.getAge(), student.getDateOfBirth() != null ? student.getDateOfBirth() : "",
                                student.getGroup() != null ? student.getGroup() : "",
                                student.getCountry() != null ? student.getCountry() : "",
                                student.getCity() != null ? student.getCity() : "",
                                student.getUniversity() != null ? student.getUniversity().Name() : ""));
            }
        }
        if (writeModules) {
            write.append("\nModules: %d\n%s".formatted(mod.length, Arrays.toString(mod)));
        }

        try (var report = new FileWriter(path)) {
            report.write(write.toString());
            System.out.printf("Report written to file %s.%n", path);
        }
    }
}

class StudentParser {
    private static final String nameField = "Фамилия Имя";
    private static final String groupField = "Группа";

    public static void setStudentData(Student student, String title, String value) {
        if (Objects.equals(title, nameField)) {
            student.setName(value);
        } else if (Objects.equals(title, groupField)) {
            student.setGroup(value);
        }
        // TODO ...
    }
}

class ModuleParser {
    private static final String activityField = "Акт";
    private static final String trainingField = "Упр";
    private static final String practiceField = "ДЗ";
    private static final String simenarField = "Сем";
    private static final String cqField = "КВ";
    private static final String trainingTaskField = "Упр:";
    private static final String practiceTaskField = "ДЗ:";
    private static final String cqTaskField = "КВ:";

    private final int numCol;
    private final Module module;

    public ModuleParser(String name, int numCol) {
        this.numCol = numCol;
        module = new Module(name);
    }

    public int getNumCol() {
        return numCol;
    }
    public Module getModule() {
        return module;
    }

    public void setStatistic(int studentId, String title, String value) {
        if (module.getStatistic(studentId) == null) {
            module.addStatistic(studentId, new ModuleStudentStatistic(module.getId(), studentId));
        }

        switch (title) {
            case trainingField -> module.getStatistic(studentId).setPoint("training", Integer.parseInt(value));
            case practiceField -> module.getStatistic(studentId).setPoint("practice", Integer.parseInt(value));
            case cqField -> module.getStatistic(studentId).setPoint("cq", Integer.parseInt(value));
            case activityField -> module.getStatistic(studentId).setPoint("activity", Integer.parseInt(value));
            case simenarField -> module.getStatistic(studentId).setPoint("simenar", Integer.parseInt(value));
        }
    }

    public void setTask(int studentId, String title, String value) {
        if (title.toUpperCase().contains(trainingTaskField.toUpperCase())) {
            var training = module.getTraining(title);
            if (training == null) {
                training = new Training(title);
                module.addTask(training);
            }
            training.addPoint(studentId, Integer.parseInt(value));
        } else if (title.contains(practiceTaskField)) {
            var practice = module.getPractice(title);
            if (practice == null) {
                practice = new Practice(title);
                module.addTask(practice);
            }
            practice.addPoint(studentId, Integer.parseInt(value));
        } else if (title.contains(cqTaskField)) {
            var cq = module.getControlQuestions(title);
            if (cq == null) {
                cq = new ControlQuestion(title);
                module.addTask(cq);
            }
            cq.addPoint(studentId, Integer.parseInt(value));
        }
    }
}