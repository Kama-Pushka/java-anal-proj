package org.ulearnstatistics;

//import com.vk.api.sdk.objects.users.responses.SearchResponse;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.commons.lang3.tuple.Triple;
import org.ulearnstatistics.db.BDRepository;
import org.ulearnstatistics.db.DBService;
import org.ulearnstatistics.db.model.TaskPointsEntity;
import org.ulearnstatistics.db.model.*;
import org.ulearnstatistics.model.Student;
import org.ulearnstatistics.parser.CSVUlearnReader;
import org.ulearnstatistics.model.Module;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class UlearnStatisticsApplication extends Application {
    /**
     * Модуль, по которому будут строяться графики. All - графики будут строиться по всем модулям.
     */
    private static String moduleFromGraphics = "All";
    /**
     * Поле баллов, по которому будут строиться графики. По дефолту - trainingPoint (баллы за упражения).
     */
    private static String pointField = "trainingPoint";
    /**
     * Определяет, будут ли баллы нормализованы в диапазон от 0 до 1.
     */
    private static boolean isPointsRelativeToMax = false;
    /**
     * Критерий, по которому будет происходить фильтрация для графиков.
     * student - имя студента (строится график успеваемости студента)
     * count - на графике остануться только те графики, в которых выборка >= полученного числа
     * none - фильтрация не проводиться
     */
    private static String criteriaType = "none";
    /**
     * Значение для фильтрации
     */
    private static String criteriaValue = "";

    /**
     * Локализация поля, по которому строится график. Нужен только для подписей.
     */
    private static String studentFieldLoc = "";
    /**
     * Локализация названия типов баллов. Нужен только для подписей
     */
    private static HashMap<String, String> pointFieldLoc = new HashMap<>() {{
        put("trainingPoint", "Упражнения");
        put("practicePoint", "Практики");
        put("cqPoint", "Вопросы");
    }};

    /**
     * Последий построенный график.
     * Нужен для одновления, в случае фильтрации или подсчета баллов относительно макс.
     */
    private static Runnable lastAction;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        openTable(stage);
    }

    private void openTable(Stage stage) throws IOException {
        var dialog = new TextInputDialog("data/java-rtf.csv");
        dialog.setTitle("Введите адрес таблицы");
        dialog.setHeaderText("Введите относительный или абсолютный адрес до вашей CSV таблицы.\nСтруктура таблицы должна соответствовать таблицам, создаваемым при выгрузке успеваемости по курсу с ulearn.me");
        dialog.setContentText("Адрес таблицы:");

        var result = dialog.showAndWait();
        if (result.isPresent()){
            setScene(stage, result.get());
        }
    }

    private void setScene(Stage stage, String csvPath) throws IOException {
        csvPath = csvPath.replace('/','\\');
        var csvFileName = csvPath.substring(csvPath.lastIndexOf(File.separator) + 1);
        csvFileName = csvFileName.substring(0, csvFileName.lastIndexOf("."));
        var bdFileName = "%s%s.db".formatted(csvPath.substring(0, csvPath.lastIndexOf(File.separator) + 1), csvFileName);

        var bd = new File(bdFileName);
        if (bd.exists() && !bd.isDirectory()) {
            DBService.updateURL(bdFileName);
        } else {
            // этап CSV парсинга
            var ulearn = readUlearnCsv(csvPath);
            var moduleList = ulearn.getModules();
            var students = ulearn.getStudents();

            // этап получения данных из VK
            addDataToStudent(students, csvFileName);

            // этап создания/работы с БД
            updateDB(bdFileName, students, moduleList);

            //ulearn.write("data/%s_report.txt".formatted(csvFileName), true, false);
        }

        var fxmlLoader = new FXMLLoader(UlearnStatisticsApplication.class.getResource("main-scene.fxml")); // TODO /main-scene.fxml
        var scene = new Scene(fxmlLoader.load(), 1280, 720);

        Node tabScene = null;
        var tabFxmlLoader = new FXMLLoader(UlearnStatisticsApplication.class.getResource("tab-view.fxml")); // TODO /tab-view.fxml
        var tabPane = (TabPane)scene.lookup("#mainTabPane");
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getId().equals("defaultTab")) {
                tab.setText(csvFileName + ".csv");
                tab.setContent(tabFxmlLoader.load());
                tabScene = tab.getContent();
            } else if (tab.getId().equals("openNewFileTab")) {
                var gif = new Image(getClass().getResourceAsStream("dezars.gif")); // TODO /dezars.gif
                var imageView = new ImageView(gif);
                var root = new VBox(imageView);
                root.setTranslateX(500);
                root.setTranslateY(250);
                tab.setContent(root);
            }
        }

        stage.setTitle("Статистика по курсам ulearn.me");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        assert tabScene != null; // TODO ??
        setButtonActions(tabScene);
    }

    private CSVUlearnReader readUlearnCsv(String csvPath) throws IOException {
        var ulearn = new CSVUlearnReader();
        ulearn.read(csvPath);
        return ulearn;
    }

    private void addDataToStudent(List<Student> students, String csvFileName) throws IOException {
//        var studInfoPath = "data/%s_student_info.txt".formatted(csvFileName); // TODO data/!!
//        HashMap<String, SearchResponse> studentDct = null;
////            var groups = new long[] { 6214974, 195681601, 22941070 };
////            var studentDct = VkRepository.getStudentDataFromVk(students, groups);
////            //serializeStudentData(studentDct, studInfoPath);
//        var student_info = new File(studInfoPath);
//        if (student_info.exists() && !student_info.isDirectory()) { // TODO чек работоспособности
//            studentDct = VkRepository.deserializeStudentData(studInfoPath);
//        }
//        if (studentDct != null) {
//            VkRepository.setStudentData(studentDct, students);
//        }
        System.out.println("Подгрузка данных с VK временно отключена");
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
        BDRepository.createTable(MaxPointModuleStatisticEntity.class, null, triple); // TODO new String[] {"moduleId"}??
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

    private void setButtonActions(Node scene) {
        var pane = (Pane)scene.lookup("#chartsHere");

        var bottomButtons = (ButtonBar)scene.lookup("#bottomButtons");
        var choiceModule = (ChoiceBox<String>)scene.lookup("#choiceModule");
        choiceModule.getItems().addAll(getModuleName());
        choiceModule.getItems().add("All");
        choiceModule.setValue("All");
        choiceModule.setOnAction(e ->
        {
            moduleFromGraphics = choiceModule.getValue();
            bottomButtons.setVisible(Objects.equals(moduleFromGraphics, "All"));
            if (lastAction != null) lastAction.run();
        });

        var main = (Button)scene.lookup("#mainButton");
        main.setText("Общая");
        Runnable mainLamda = (() -> BarChart(pane, getMainStatistic(moduleFromGraphics, pointField, isPointsRelativeToMax)));
        main.setOnAction(e->{ mainLamda.run(); lastAction=mainLamda; } );

        var sex = (Button)scene.lookup("#sexButton");
        sex.setText("Пол");
        Runnable sexLamda = (() -> LineChart(pane, getStatisticForLineChart(moduleFromGraphics,"sex", pointField, isPointsRelativeToMax, criteriaType, criteriaValue)));
        sex.setOnAction(e->{ studentFieldLoc ="Пол"; sexLamda.run(); lastAction=sexLamda; } );

        var group = (Button)scene.lookup("#groupButton");
        group.setText("Группа");
        Runnable groupLamda = (() -> LineChart(pane, getStatisticForLineChart(moduleFromGraphics,"group", pointField, isPointsRelativeToMax, criteriaType, criteriaValue)));
        group.setOnAction(e->{ studentFieldLoc ="Группа"; groupLamda.run(); lastAction=groupLamda; } );

        var city = (Button)scene.lookup("#cityButton");
        city.setText("Город");
        Runnable cityLamda = (() -> LineChart(pane, getStatisticForLineChart(moduleFromGraphics,"city", pointField, isPointsRelativeToMax, criteriaType, criteriaValue)));
        city.setOnAction(e->{ studentFieldLoc ="Город"; cityLamda.run(); lastAction=cityLamda; } );

        var training = (Button)scene.lookup("#trainingButton");
        training.setText("Упражнения");
        training.setOnAction(e -> {
            pointField ="trainingPoint";lastAction.run();});

        var practice = (Button)scene.lookup("#practiceButton");
        practice.setText("Практики");
        practice.setOnAction(e -> {
            pointField ="practicePoint";lastAction.run();});

        var cq = (Button)scene.lookup("#cqButton");
        cq.setText("Вопросы");
        cq.setOnAction(e -> {
            pointField ="cqPoint";lastAction.run();});

        var max = (CheckBox)scene.lookup("#maxButton");
        max.setText("Относ. макс.");
        max.setOnAction(e -> {
            isPointsRelativeToMax =!isPointsRelativeToMax;
            if (lastAction != null) lastAction.run();
        });

        var filter = (Button)scene.lookup("#filterButton");
        var filterCriteria = (TextField)scene.lookup("#filterCriteria");
        var graphicButtons = (ButtonBar)scene.lookup("#graphicTypeButtons");
        var filterType = (ChoiceBox<String>)scene.lookup("#filterType");
        filterType.getItems().add("student");
        filterType.getItems().add("count");
        filterType.getItems().add("none");
        filterType.setValue("none");
        filterType.setOnAction(e ->
        {
            criteriaType = filterType.getValue();
            graphicButtons.setVisible(!Objects.equals(criteriaType, "student"));
            filter.setDisable(Objects.equals(criteriaType, "none"));

            if (filterType.getValue().equals("count")) {
                filterCriteria.setPromptText("Введите число...");
            } else if (filterType.getValue().equals("student")) {
                filterCriteria.setPromptText("Введите имя...");
            } else if (filterType.getValue().equals("none")) {
                filterCriteria.setPromptText("");
            }
        });

        filter.setOnAction(e -> {
            if (filterType.getValue().equals("count")) {
                criteriaValue =filterCriteria.getText();
                lastAction.run();
            } else if (filterType.getValue().equals("student")) {
                criteriaValue =filterCriteria.getText();
                Runnable studentLambda = (() -> LineChart(pane, getStatisticForLineChart(moduleFromGraphics,"name", pointField, isPointsRelativeToMax, criteriaType, criteriaValue)));
                studentLambda.run(); // TODO работает только с полем name (добавить surname или убрать его вообще + переделать добавление данных в бд)
                lastAction=studentLambda;
            } else if (filterType.getValue().equals("none")) {
                criteriaValue =filterCriteria.getText();
            }
        });
        filter.setText("Фильтровать");
        filter.setDisable(true);
    }

    private static List<String> getModuleName() {
        return DBService.getDataFromTable("SELECT name FROM ModuleEntity");
    }

    private Map<String, Map<String, Double>> getStatisticForLineChart(String module, String field, String pointName, boolean max, String filter, String filterCriteria) {
        return BDRepository.getAVGForFieldForLineChart(module, field, pointName, max, filter, filterCriteria);
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

        for (var k : map.keySet()) { // MALE, FEMALE
            for (var km : map.get(k).keySet()) { // ООП, За весь курс...
                if (!k.equals("null")) {
                    lines.get(k).getData().add(new XYChart.Data<>(km, map.get(k).get(km)));
                }
            }
        }

        var xAxis = new CategoryAxis();
        var yAxis = new NumberAxis();
        yAxis.setLabel(isPointsRelativeToMax ? "Баллы / Макс. баллы" : "Количество баллов");

        var lineChart = new LineChart<>(xAxis, yAxis);
        if (criteriaType.equals("student")) {
            lineChart.setTitle(Objects.equals(moduleFromGraphics, "All")
                    ? "Среднее количество баллов у студента \"%s\" относительно %s".formatted(criteriaValue, pointFieldLoc.get(pointField))
                    : "Среднее количество баллов у студента \"%s\" в модуле \"%s\"".formatted(criteriaValue, moduleFromGraphics));
        } else {
            lineChart.setTitle(Objects.equals(moduleFromGraphics, "All")
                    ? "Среднее количество баллов за %s относительно %s".formatted(pointFieldLoc.get(pointField), studentFieldLoc)
                    : "Среднее количество баллов относительно %s в модуле \"%s\"".formatted(studentFieldLoc, moduleFromGraphics));
        }

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
    }

    private HashMap<String, Double> getMainStatistic(String moduleName, String fieldName, boolean max) {
        return BDRepository.getAVGForField(moduleName, fieldName, max);
    }

    private void BarChart(Pane root, HashMap<String, Double> map) {
        var data = new XYChart.Series<String, Number>();
        for (var k : map.keySet()) {
            data.getData().add(new XYChart.Data<>(k, map.get(k)));
        }

        var xAxis = new CategoryAxis();
        var yAxis = new NumberAxis();
        yAxis.setLabel(isPointsRelativeToMax ? "Баллы / Макс. баллы" : "Количество баллов");

        var barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle(Objects.equals(moduleFromGraphics, "All")
                ? "Среднее количество баллов за %s по модулям".formatted(pointFieldLoc.get(pointField))
                : "Среднее количество баллов в модуле \"%s\"".formatted(moduleFromGraphics));

        barChart.getData().add(data);
        barChart.setLegendVisible(false);

        barChart.setMinSize(1150, 530); // TODO ??
        barChart.setMaxSize(1150, 530); // TODO ??
        barChart.setLayoutX(10);
        barChart.setLayoutY(10);

        root.getChildren().clear();
        root.getChildren().add(barChart);
    }

    /**
     * НЕ ИСПОЛЬЗУЕТСЯ
     */
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

    /**
     * НЕ ИСПОЛЬЗУЕТСЯ
     */
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
}