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

    private int studentId;
    private int moduleId;

    /**
     * Открывает и парсит CSV файл по заданному пути.
     * CSV файл обязательно должен иметь структуру CSV файла выгрузки успеваемости по курсу с ulearn.me.
     * Полученные данные сохраняются в массивах students и modules данного класса.
     * @param path Путь до файла
     * @throws IOException
     */
    public void read(String path) throws IOException {
        var scanner = new Scanner(new File(path));

        var modules = readModules(scanner.nextLine());
        var studentColsNum = modules.stream().findFirst().get().getNumCol();
        modules.remove(0);
        var titles = scanner.nextLine().split(";");
        var maxValues = Arrays.stream(scanner.nextLine().split(";")).skip(studentColsNum).toArray(String[]::new);

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

    /**
     * Читает и парсит заголовок модулей файла.
     * @param line Строка с заголовками модулей.
     * @return Список ModuleParser, в котором содержатся модули и сколько колонок занимает модуль в таблице.
     * Первым модулем всегда будет Student, необходим для парсинга данных о студентах.
     */
    private ArrayList<ModuleParser> readModules(String line) {
        var moduleNames = line.split(";", -1);
        moduleNames[0] = ""; // for UTF8-BOM
        var moduleName = "Students";
        var modules = new ArrayList<ModuleParser>();

        var lastModuleIndex = 0;
        for (var i = 0; i < moduleNames.length; i++) {
            var numCols = i - lastModuleIndex;
            if (!Objects.equals(moduleNames[i], "")) {
                modules.add(new ModuleParser(moduleName, moduleId, numCols));
                if (!Objects.equals(moduleName, "Students"))
                    moduleId += numCols;
                moduleName = moduleNames[i];
                lastModuleIndex = i;
            }

            if (i == moduleNames.length - 1) {
                modules.add(new ModuleParser(moduleName, moduleId, numCols + 1));
            }
        }
        return modules;
    }

    /**
     * Парсинг данных о студенте.
     * @param titles Заголовки колонок
     * @param data Данные о студенте для парсинга
     * @return Новый экземпляр студента, в котором содержится полученная информация
     */
    private Student readStudentData(String[] titles, String[] data) {
        var student = new Student(studentId++);
        for (var i = 0; i < data.length; i++) {
            StudentParser.setStudentData(student, titles[i], data[i]);
        }
        return student;
    }

    /**
     * Парсит информацию о баллах студента по заданиям курса.
     * @param student Студент, данные которого обрабатываются
     * @param titles Список заголовков названий заданий
     * @param attrs Список баллов для парсинга
     * @param modules Список модулей, по которым необходимо парсить
     * @param maxPoints Список максимальных количеств баллов для каждого задания
     */
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

    /**
     * Записывает полученные данные в файл.
     * @param path Путь до файла
     * @param writeStudents Нужно ли записывать информацию о студентах
     * @param writeModules Нужно ли записывать информацию о модулях
     * @throws IOException
     */
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

    /**
     * Устанавливает полученные данные в студента.
     * @param student Студент
     * @param title Имя студента
     * @param value Группа студента
     */
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

    public ModuleParser(String name, int id, int numCol) {
        this.numCol = numCol;
        module = new Module(name, id);
    }

    public int getNumCol() {
        return numCol;
    }
    public Module getModule() {
        return module;
    }

    /**
     * Устанавливает макс. количество баллов для полей общей статистики по заданиям в модуле.
     * @param title Нзвание поля
     * @param value Макс. значение
     */
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

    /**
     * Устанавливает, сколько баллов получил студент по полям общей статистики по заданиям в модуле.
     * @param studentId Внутренний ID студента
     * @param title Название поля
     * @param value Значение
     */
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

    /**
     * Устанавливает, сколько баллов получил студент за задание.
     * @param studentId Внутренний ID студента
     * @param title Название задания
     * @param value Значение
     * @param index Порядковый номер колонки в таблице (необходим для дальнейшего правильного парсинга)
     * @param maxPoints Макс. баллы, которые можно получить за задание (необходимо при инициализации задания)
     */
    public void setTask(int studentId, String title, String value, int index, String[] maxPoints) {
        if (title.toLowerCase().contains(trainingTaskField)) {
            var training = module.getTraining(title + " | " + index);
            if (training == null) {
                training = new Training(title + " | " + index, index);
                training.setMaxPoint(Integer.parseInt(maxPoints[index]));
                module.addTask(training);
            }
            training.addPoint(studentId, Integer.parseInt(value));
        } else if (title.toLowerCase().contains(practiceTaskField)) {
            var practice = module.getPractice(title + " | " + index);
            if (practice == null) {
                practice = new Practice(title + " | " + index, index);
                practice.setMaxPoint(Integer.parseInt(maxPoints[index]));
                module.addTask(practice);
            }
            practice.addPoint(studentId, Integer.parseInt(value));
        } else if (title.toLowerCase().contains(cqTaskField)) {
            var cq = module.getControlQuestions(title + " | " + index);
            if (cq == null) {
                cq = new ControlQuestion(title + " | " + index, index);
                cq.setMaxPoint(Integer.parseInt(maxPoints[index]));
                module.addTask(cq);
            }
            cq.addPoint(studentId, Integer.parseInt(value));
        }
    }
}