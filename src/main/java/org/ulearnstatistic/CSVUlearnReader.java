package org.ulearnstatistic;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.commons.lang3.tuple.Pair;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class CSVUlearnReader {
    private static final String nameField = "Фамилия Имя";
    private static final String groupField = "Группа";
    private static final String activityField = "Акт";
    private static final String trainingField = "Упр";
    private static final String practiceField = "ДЗ";
    private static final String simenarField = "Сем";
    private static final String cqField = "КВ";
    private static final String trainingTaskField = "Упр:";
    private static final String practiceTaskField = "ДЗ:";
    private static final String cqTaskField = "КВ:";

    private final ArrayList<Student> students = new ArrayList<>();
    private final ArrayList<Module> modules = new ArrayList<>();

    public ArrayList<Student> getStudents() {
        return students; // TODO students.copy?
    }
    public ArrayList<Module> getModules() {
        return modules; // TODO modules.copy?
    }

    public void read(String path) throws IOException, CsvValidationException {
        //var reader = new CSVReader(new InputStreamReader(new FileInputStream("data.csv"), StandardCharsets.UTF_8));

        var filereader = new FileReader(path);
        var csv = new CSVReaderBuilder(filereader).withCSVParser(new CSVParserBuilder().withSeparator(';').build()).build(); // .withSkipLines(1)

        var moduleConf = getModuleConfiguration(csv);
        var titles = csv.readNext(); // 2
        var maxValues = csv.readNext(); // 3    TODO добавить в статистику

        String[] nextRecord;
        while ((nextRecord = csv.readNext()) != null) {
            var student = new Student();
            students.add(student); // TODO переделать добавление в список?

            var index = 0;
            var currModule = 0;
            Module module;
            for (var i = 0; i < nextRecord.length; i++) {
                if (i - index == moduleConf.get(currModule).getValue()) {
                    currModule++;
                    index = i;
                }

                module = moduleConf.get(currModule).getKey();
                if (Objects.equals(module.getName(), "Student")) { // TODO это костыль для записи студентов
                    setStudentData(student, titles[i], nextRecord[i]);
                }
                else {
                    if (i-index==0) {
                        module.addStatistic(student.getId(),new ModuleStudentStatistic(student.getId(),module.getId()));
                    }
                    setStatistic(module, student.getId(), titles[i], nextRecord[i]);
                    setTaks(module, titles[i], nextRecord[i], student);
                }
            }
        }
        filereader.close();
        csv.close();
    }

    private ArrayList<Pair<Module,Integer>> getModuleConfiguration(CSVReader csv) throws CsvValidationException, IOException {
        var cells = csv.readNext(); // 1

        var modulesLengthConf = new ArrayList<Pair<Module,Integer>>();
        var module = new Module("Student"); // fake module
        var index = 0;
        for (var i = 1; i < cells.length; i++) {
            if (!Objects.equals(cells[i], "п»ї") && !Objects.equals(cells[i], "")) { // КОСТЫЛЬ ДЛЯ ФОРМАТА UTF-8-BOM
                modulesLengthConf.add(Pair.of(module, i - index));

                module = new Module(cells[i]);
                modules.add(module); // TODO переделать добавление в список?
                index = i;
            }
            else if (i == cells.length - 1) {
                modulesLengthConf.add(Pair.of(module, i - index + 1)); // КОСТЫЛЬ? для последнего элемента
            }
        }

        return modulesLengthConf;
    }

    private void setStudentData(Student student, String title, String value) {
        if (Objects.equals(title, nameField)) {
            student.setName(value);
        } else if (Objects.equals(title, groupField)) {
            student.setGroup(value);
        }
        // TODO ...
    }

    private void setStatistic(Module module, int studentId, String title, String value) { // TODO двойная проверка получается
        switch (title) {
            case trainingField -> module.getStatistic(studentId).setPoint("training", Integer.parseInt(value));
            case practiceField -> module.getStatistic(studentId).setPoint("practice", Integer.parseInt(value));
            case cqField -> module.getStatistic(studentId).setPoint("cq", Integer.parseInt(value));
            case activityField -> module.getStatistic(studentId).setPoint("activity", Integer.parseInt(value));
            case simenarField -> module.getStatistic(studentId).setPoint("simenar", Integer.parseInt(value));
        }
    }

    private void setTaks(Module module, String title, String value, Student student) {
        if (title.contains(trainingTaskField)) { // TODO IgnoreCase
            var training = module.getTraining(title);
            if (training == null) {
                training = new Training(title);
                module.addTask(training);
            }
            training.addPoint(student.getName(), Integer.parseInt(value)); // TODO student.getId()
        } else if (title.contains(practiceTaskField)) {
            var practice = module.getPractice(title);
            if (practice == null) {
                practice = new Practice(title);
                module.addTask(practice);
            }
            practice.addPoint(student.getName(), Integer.parseInt(value)); // TODO student.getId()
        } else if (title.contains(cqTaskField)) {
            var cq = module.getControlQuestions(title);
            if (cq == null) {
                cq = new ControlQuestion(title);
                module.addTask(cq);
            }
            cq.addPoint(student.getName(), Integer.parseInt(value)); // TODO student.getId()
        }
    }

    public void write(String path) throws IOException {
        var write = new StringBuilder();
        var stud = students.toArray(); // System.out.println(modules.toArray().length);
        var mod = modules.toArray(); // System.out.println(students.toArray().length);
        write.append("Students: %d\n%s".formatted(stud.length, Arrays.toString(stud))); // System.out.println(Arrays.toString(moduleList.toArray()));
        write.append("\nModules: %d\n%s".formatted(mod.length, Arrays.toString(mod))); // System.out.println(Arrays.toString(moduleList.toArray()));

        try (var report = new FileWriter(path)) {
            report.write(write.toString());
            System.out.printf("Report written to file %s.%n", path);
        }
    }
}
