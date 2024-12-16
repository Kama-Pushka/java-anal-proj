package org.ulearnstatistic;

import com.vk.api.sdk.objects.users.responses.SearchResponse;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.apache.commons.lang3.tuple.Triple;
import org.ulearnstatistic.db.BDRepository;
import org.ulearnstatistic.db.DBService;
import org.ulearnstatistic.db.TaskPointsEntity;
import org.ulearnstatistic.db.model.*;
import org.ulearnstatistic.model.Student;
import org.ulearnstatistic.parser.CSVUlearnReader;
import org.ulearnstatistic.vkApi.VkRepository;
import org.ulearnstatistic.model.Module;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class HelloApplication extends Application {
    private static String test = "All";
    private static String test2 = "trainingPoint";
    private static boolean test3 = false;
    private static String test4 = "";
    private static boolean test5 = false;

    private static Runnable lastAction;

    @Override
    public void start(Stage stage) throws IOException {
        var dialog = new TextInputDialog("data/java-rtf.csv");
        dialog.setTitle("Введите адрес таблицы");
        dialog.setHeaderText("Введите относительный или абсолютный адрес до вашей CSV таблицы. Структура таблицы должна соответствовать таблицам, создаваемым при выгрузке таблиц с ulearn.me");
        dialog.setContentText("Адрес таблицы:");

        var result = dialog.showAndWait();
        if (result.isPresent()){
            setScene(stage, result.get());
        }
    }

    private void setScene(Stage stage, String csvPath) throws IOException {
        var csvTableName = Arrays.stream(csvPath.split("[/\\\\]")).findFirst().get().split("\\.")[0]; // TODO проверить на пустое значение

        var bdFileName = "%s.bd".formatted(csvTableName);
        var bd = new File(bdFileName);
        if (bd.exists() && !bd.isDirectory()) {
            DBService.updateURL(bdFileName); // "C:\\Users\\Ser12\\Desktop\\java\\java-anal-proj\\data\\java-rtf.db"
        } else {
            // этап CSV парсинга
            var ulearn = readUlearnCsv(csvPath);
            var moduleList = ulearn.getModules();
            var students = ulearn.getStudents();

            // этап получения данных из VK
            addDataToStudent(students);

            // этап создания/работы с БД
            updateDB(bdFileName, students, moduleList);

            //ulearn.write("data\\report.txt", true, false); // TODO переписать чтоюы выводил полностью таблицу
        }

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 720);

        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        setButtonActions(scene);
        //LineChart(stage);
    }

    private CSVUlearnReader readUlearnCsv(String csvPath) throws IOException {
        var ulearn = new CSVUlearnReader();
        ulearn.read(csvPath); // "data/java-rtf.csv"
        return ulearn;
    }

    private void addDataToStudent(List<Student> students) throws IOException {
        HashMap<String, SearchResponse> studentDct = null;
//            var groups = new long[] { 6214974, 195681601, 22941070 };
//            var studentDct = VkRepository.getStudentDataFromVk(students, groups);
//            //serializeStudentData(studentDct,"data\\student_info.txt");
        var studInfoPath = "data/student_info.txt"; // для java-rtf.csv TODO !!
        var student_info = new File(studInfoPath);
        if (student_info.exists() && !student_info.isDirectory()) { // TODO чек работоспособности
            studentDct = VkRepository.deserializeStudentData(studInfoPath);
        }
        if (studentDct != null) {
            VkRepository.setStudentData(studentDct, students);
        }
    }

    private void updateDB(String dbPath, List<Student> students, List<Module> moduleList) {
        DBService.updateURL(dbPath); // "data/java-rtf.db"
        DBService.connect();

        // кладем в бд и получаем из нее информацию о студентах
        var studentEntities = new ArrayList<StudentEntity>();
        for (var student : students) {
            studentEntities.add(new StudentEntity(student));
        }
        saveStudentsInDB(studentEntities);

        // кладем в бд и получаем из нее информацию о модулях
        var modules = new ArrayList<ModuleEntity>();
        var maxPointModule = new ArrayList<MaxPointModuleStatisticEntity>();
        var moduleStudentStatistic = new ArrayList<ModuleStudentStatisticEntity>();
        var tasks = new ArrayList<TaskEntity>();
        var taskPointsEntity = new ArrayList<TaskPointsEntity>();
        BDRepository.parseModelToDataModel(moduleList, maxPointModule, modules, moduleStudentStatistic, tasks, taskPointsEntity);

        saveModuleInDB(modules);
        saveMaxPointModulesInDB(maxPointModule);
        saveModuleStudentStatisticsInDB(moduleStudentStatistic);
        saveTasksInDB(tasks);
        saveTaskPointsInDB(taskPointsEntity);
    }

    private List<?> saveStudentsInDB(List<StudentEntity> studentEntities) {
        BDRepository.createTable(StudentEntity.class, new String[] {"id"}, null);
        BDRepository.saveIntoTable(StudentEntity.class, studentEntities);
        return BDRepository.getAllDataFromTable(StudentEntity.class);
    }

    private List<?> saveModuleInDB(List<ModuleEntity> modules) {
        BDRepository.createTable(ModuleEntity.class, new String[] {"id"}, null);
        BDRepository.saveIntoTable(ModuleEntity.class, modules);
        return BDRepository.getAllDataFromTable(ModuleEntity.class);
    }

    private List<?> saveMaxPointModulesInDB(List<MaxPointModuleStatisticEntity> maxPointModule) {
        var triple = new Triple[] {
                Triple.of("moduleId", "ModuleEntity", "id"),
        };
        BDRepository.createTable(MaxPointModuleStatisticEntity.class, null, triple); // new String[] {"id"}??
        BDRepository.saveIntoTable(MaxPointModuleStatisticEntity.class, maxPointModule);
        return BDRepository.getAllDataFromTable(MaxPointModuleStatisticEntity.class);
    }

    private List<?> saveModuleStudentStatisticsInDB(List<ModuleStudentStatisticEntity> moduleStudentStatistic) {
        var triple = new Triple[] {
                Triple.of("moduleId", "ModuleEntity", "id"),
                Triple.of("studentId", "StudentEntity", "id")
        };
        BDRepository.createTable(ModuleStudentStatisticEntity.class, new String[] {"moduleId, studentId"}, triple);
        BDRepository.saveIntoTable(ModuleStudentStatisticEntity.class, moduleStudentStatistic);
        return BDRepository.getAllDataFromTable(ModuleStudentStatisticEntity.class);
    }

    private List<?> saveTasksInDB(List<TaskEntity> tasks) {
        var triple = new Triple[] {
                Triple.of("moduleId", "ModuleEntity", "id")
        };
        BDRepository.createTable(TaskEntity.class, new String[] {"id"}, triple);
        BDRepository.saveIntoTable(TaskEntity.class, tasks);
        return BDRepository.getAllDataFromTable(TaskEntity.class);
    }

    private List<?> saveTaskPointsInDB(List<TaskPointsEntity> taskPointsEntity) {
        var triple = new Triple[] {
                Triple.of("taskId", "TaskEntity", "id"),
                Triple.of("studentId", "StudentEntity", "id")
        };
        BDRepository.createTable(TaskPointsEntity.class, new String[] {"taskId,  studentId"}, triple);
        BDRepository.saveIntoTable(TaskPointsEntity.class, taskPointsEntity);
        return BDRepository.getAllDataFromTable(TaskPointsEntity.class);
    }

    private void setButtonActions(Scene scene) {
        var pane = (Pane)scene.lookup("#chartsHere");
//        var buttonBar = (ButtonBar)scene.lookup("#gay");
//        var typeButtonBar = (ButtonBar)scene.lookup("#typeBB");
//        var button1 = pane.lookup("#mainButton");
//        var button = buttonBar.getButtons().stream().findFirst().get();
        //var btn = (Button)button;
        //btn.setOnAction(e -> BarChart(pane, getMainStatistic("practicePoint")));

        var bottomButtons = (ButtonBar)scene.lookup("#bottomButtons");
        var choiceModule = (ChoiceBox<String>)scene.lookup("#choiceModule");
        choiceModule.getItems().addAll(getModuleName());
        choiceModule.getItems().add("All");
        choiceModule.setValue("All");
        choiceModule.setOnAction(e ->
        {
            test = choiceModule.getValue();
            bottomButtons.setVisible(Objects.equals(test, "All"));
        });

        var main = (Button)scene.lookup("#mainButton");
        Runnable mainLamda = (() -> BarChart(pane, getMainStatistic(test,test2, test3)));
        main.setOnAction(e->{ mainLamda.run(); lastAction=mainLamda; } );

        var sex = (Button)scene.lookup("#sexButton");
        Runnable sexLamda = (() -> LineChart(pane, getStatisticForLineChart(test,"sex", test2, test3, test5)));
        sex.setOnAction(e->{ test4="Пол"; sexLamda.run(); lastAction=sexLamda; } );

        var group = (Button)scene.lookup("#groupButton");
        Runnable groupLamda = (() -> LineChart(pane, getStatisticForLineChart(test,"group",test2, test3, test5)));
        group.setOnAction(e->{ test4="Группа"; groupLamda.run(); lastAction=groupLamda; } );

        var city = (Button)scene.lookup("#cityButton");
        Runnable cityLamda = (() -> LineChart(pane, getStatisticForLineChart(test,"city",test2, test3, test5)));
        city.setOnAction(e->{ test4="Город"; cityLamda.run(); lastAction=cityLamda; } );

        //// TODO убирать эти кнопки, когда смотрим по модулям      //TODO отсеять те группы или города, где менее 3 человек
        var training = (Button)scene.lookup("#trainingButton");
        training.setOnAction(e -> {test2="trainingPoint";lastAction.run();});

        var practice = (Button)scene.lookup("#practiceButton");
        practice.setOnAction(e -> {test2="practicePoint";lastAction.run();});

        var cq = (Button)scene.lookup("#cqButton");
        cq.setOnAction(e -> {test2="cqPoint";lastAction.run();});

        var max = (Button)scene.lookup("#maxButton");
        max.setOnAction(e -> {test3=!test3;lastAction.run();});

        var filter = (Button)scene.lookup("#filterButton");
        filter.setOnAction(e -> {test5=!test5;lastAction.run();});
    }

    private static List<String> getModuleName() {
        return DBService.getDataFromTable("SELECT name FROM ModuleEntity");
    }

    private Map<String, Map<String, Double>> getStatisticForLineChart(String module, String field, String pointName, boolean max, boolean filter) {
        return BDRepository.getAVGForFieldForLineChart(module, field, pointName, max, filter);
    }

    // Map><String (модуль - задания-x), Map<String (пол-линия), Integer (значение-y)>> map
    private void LineChart(Pane root, Map<String, Map<String, Double>> map) {
        // TODO костыль?
        var lines = new HashMap<String, XYChart.Series<String, Number>>();
        for (var key : map.keySet()) {
            var data = new XYChart.Series<String, Number>();
            data.setName(key);
            lines.put(key, data);
        }

        for (var k : map.keySet()) { // MALE FEMALE
            for (var km : map.get(k).keySet()) { // ООП За весь курс...
                if (!k.equals("null")) {
                    lines.get(k).getData().add(new XYChart.Data<>(km, map.get(k).get(km)));
                }
            }
        }

//        var root = new HBox();
//        var scene = new Scene(root, 450, 330);

        var xAxis = new CategoryAxis();
        //xAxis.setLabel("Age");

        var yAxis = new NumberAxis();
        yAxis.setLabel(test3 ? "Баллы / Макс. баллы" : "Количество баллов");

        var lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Среднее количество %s баллов относительно %s".formatted(test2, test4));

//        data.getData().add(new XYChart.Data<>(18, 567));
//        data.getData().add(new XYChart.Data<>(20, 612));
//        data.getData().add(new XYChart.Data<>(25, 800));
//        data2.getData().add(new XYChart.Data<>(30, 980));
//        data2.getData().add(new XYChart.Data<>(40, 1410));
//        data2.getData().add(new XYChart.Data<>(50, 2350));

        for (var line : lines.values()) {
            lineChart.getData().add(line);
        }

        lineChart.setMinSize(1150, 530); // TODO ??
        lineChart.setMaxSize(1150, 530); // TODO ??
        lineChart.setLayoutX(10);
        lineChart.setLayoutY(10);
        lineChart.setLegendSide(Side.RIGHT);

        root.getChildren().clear();
        root.getChildren().add(lineChart);

//        stage.setTitle("LineChart");
//        stage.setScene(scene);
//        stage.show();
    }

    private void ScatterChart(Stage stage) {

        var root = new HBox();

        var xAxis = new CategoryAxis();
        var yAxis = new NumberAxis("USD/kg", 30, 50, 2);

        var scatterChart = new ScatterChart<>(xAxis, yAxis);

        var data = new XYChart.Series<String, Number>();

        data.getData().add(new XYChart.Data<>("Mar 14", 43));
        data.getData().add(new XYChart.Data<>("Nov 14", 38.5));
        data.getData().add(new XYChart.Data<>("Jan 15", 41.8));
        data.getData().add(new XYChart.Data<>("Mar 15", 37));
        data.getData().add(new XYChart.Data<>("Dec 15", 33.7));
        data.getData().add(new XYChart.Data<>("Feb 16", 39.8));

        scatterChart.getData().add(data);
        scatterChart.setLegendVisible(false);

        var scene = new Scene(root, 450, 330);
        root.getChildren().add(scatterChart);

        stage.setTitle("Gold price");
        stage.setScene(scene);
        stage.show();
    }

    private HashMap<String, Double> getMainStatistic(String moduleName, String fieldName, boolean max) {
        return BDRepository.getAVGForField(moduleName, fieldName, max);
    }

    private void BarChart(Pane root, HashMap<String, Double> map) {

        var data = new XYChart.Series<String, Number>();
        for (var k : map.keySet()) {
            data.getData().add(new XYChart.Data<>(k, map.get(k)));
        }

        //var root1 = new HBox();
        //var scene = new Scene(root, 1200, 830);

        var xAxis = new CategoryAxis();
        var yAxis = new NumberAxis();
        yAxis.setLabel(test3 ? "Баллы / Макс. баллы" : "Количество баллов");

        var barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle(Objects.equals(test, "All")
                ? "Среднее количество %s баллов по модулям".formatted(test2)
                : "Среднее количество %s баллов в модуле \"%s\"".formatted(test2, test));

//        data.getData().add(new XYChart.Data<>("China", 38));
//        data.getData().add(new XYChart.Data<>("UK", 29));
//        data.getData().add(new XYChart.Data<>("Russia", 22));
//        data.getData().add(new XYChart.Data<>("South Korea", 13));
//        data.getData().add(new XYChart.Data<>("Germany", 11));

        //xAxis.tickLabelRotationProperty().set(340);
        //xAxis.tickLengthProperty().set(0);

        barChart.getData().add(data);
        barChart.setLegendVisible(false);

        barChart.setMinSize(1150, 530); // TODO ??
        barChart.setMaxSize(1150, 530); // TODO ??
        barChart.setLayoutX(10);
        barChart.setLayoutY(10);
        //barChart.setStyle("-fx-font-size: " + 10 + "px;");

        root.getChildren().clear();
        root.getChildren().add(barChart);

//        var tab = root.getTabs().stream().findFirst().get();
//        var archor = (AnchorPane) tab.getContent();
//        archor.getChildren().add(barChart);
//        tab.setContent(archor);

//        stage.setTitle("BarChart");
//        stage.setScene(scene);
//        stage.show();
    }

    private void PieChart(Stage stage) {

        var root = new HBox();

        var scene = new Scene(root, 450, 330);

        ObservableList<PieChart.Data> pieChartData
                = FXCollections.observableArrayList(
                new PieChart.Data("Apache", 52),
                new PieChart.Data("Nginx", 31),
                new PieChart.Data("IIS", 12),
                new PieChart.Data("LiteSpeed", 2),
                new PieChart.Data("Google server", 1),
                new PieChart.Data("Others", 2));

        var pieChart = new PieChart(pieChartData);
        pieChart.setTitle("Web servers market share (2016)");

        root.getChildren().add(pieChart);

        stage.setTitle("PieChart");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}