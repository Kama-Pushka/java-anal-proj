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
        var studentColsNum = modules.stream().findFirst().get().getNumCol();
        modules.remove(0);
        var titles = scanner.nextLine().split(";");
        var maxValues = Arrays.stream(scanner.nextLine().split(";")).skip(studentColsNum).toArray(String[]::new); // TODO использовать     TODO проверить!!

        var titleModulesData = Arrays.copyOfRange(titles,studentColsNum,titles.length);
        var col = 0;
        for (var module : modules) {
            for (var i = 0; i < module.getNumCol(); i++, col++) {
                module.setMaxPoints(titleModulesData[col], maxValues[col]);
            }
        }

        students = new ArrayList<Student>();
        while (scanner.hasNextLine()) {
            var line = scanner.nextLine().split(";");
            var studentData = Arrays.copyOfRange(line,0,studentColsNum);
            var modulesData = Arrays.copyOfRange(line,studentColsNum,line.length);

            var student = readStudentData(titles,studentData);
            students.add(student);
            readModulesTasksData(student,titleModulesData,modulesData,modules,maxValues);
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

    private void readModulesTasksData(Student student, String[] titles, String[] attrs, ArrayList<ModuleParser> modules,
                                      String[] maxPoints) {
        var m = new ArrayDeque<ModuleParser>(modules); // TODO
        var module = m.pop();

        var lastModuleIndex = 0;
        for (var i = 0; i < attrs.length; i++) {
            if (i - lastModuleIndex == module.getNumCol()) {
                module = m.pop();
                lastModuleIndex = i;
            }

            module.setStatistic(student.getId(), titles[i], attrs[i]); // общая статистика студента по типу задания (если это оно)
            module.setTask(student.getId(), titles[i], attrs[i], i, maxPoints); // статистика студента по конкретному заданию (если это оно)
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
    private static final String activityField = "акт";
    private static final String trainingField = "упр";
    private static final String practiceField = "дз";
    private static final String simenarField = "сем";
    private static final String cqField = "кв";
    private static final String trainingTaskField = "упр:";
    private static final String practiceTaskField = "дз:";
    private static final String cqTaskField = "кв:";

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

    public void setMaxPoints(String title, String value) {
        if (module.getMaxPoints() == null) { // TODO так то не надо
            module.setMaxPoints(new MaxPointModuleStatistic(module.getId()));
        }

        switch (title.toLowerCase()) {
            case trainingField -> module.getMaxPoints().setPoint("training", Integer.parseInt(value));
            case practiceField -> module.getMaxPoints().setPoint("practice", Integer.parseInt(value));
            case cqField -> module.getMaxPoints().setPoint("cq", Integer.parseInt(value));
            case activityField -> module.getMaxPoints().setPoint("activity", Integer.parseInt(value));
            case simenarField -> module.getMaxPoints().setPoint("simenar", Integer.parseInt(value));
        }
    }

    public void setStatistic(int studentId, String title, String value) {
        if (module.getStatistic(studentId) == null) {
            module.addStatistic(studentId, new ModuleStudentStatistic(module.getId(), studentId));
        }

        switch (title.toLowerCase()) {
            case trainingField -> module.getStatistic(studentId).setPoint("training", Integer.parseInt(value));
            case practiceField -> module.getStatistic(studentId).setPoint("practice", Integer.parseInt(value));
            case cqField -> module.getStatistic(studentId).setPoint("cq", Integer.parseInt(value));
            case activityField -> module.getStatistic(studentId).setPoint("activity", Integer.parseInt(value));
            case simenarField -> module.getStatistic(studentId).setPoint("simenar", Integer.parseInt(value));
        }
    }

    public void setTask(int studentId, String title, String value, int index, String[] maxPoints) {
        if (title.toLowerCase().contains(trainingTaskField)) {
            var training = module.getTraining(title + " | " + index);
            if (training == null) {
                training = new Training(title + " | " + index);
                training.setMaxPoint(Integer.parseInt(maxPoints[index]));
                module.addTask(training);
            }
            training.addPoint(studentId, Integer.parseInt(value));
        } else if (title.toLowerCase().contains(practiceTaskField)) {
            var practice = module.getPractice(title + " | " + index);
            if (practice == null) {
                practice = new Practice(title + " | " + index);
                practice.setMaxPoint(Integer.parseInt(maxPoints[index]));
                module.addTask(practice);
            }
            practice.addPoint(studentId, Integer.parseInt(value));
        } else if (title.toLowerCase().contains(cqTaskField)) {
            var cq = module.getControlQuestions(title + " | " + index);
            if (cq == null) {
                cq = new ControlQuestion(title + " | " + index);
                cq.setMaxPoint(Integer.parseInt(maxPoints[index]));
                module.addTask(cq);
            }
            cq.addPoint(studentId, Integer.parseInt(value));
        }
    }
}