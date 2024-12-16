package org.ulearnstatistic.vkApi;

import com.google.gson.Gson;
import com.vk.api.sdk.objects.users.OccupationType;
import com.vk.api.sdk.objects.users.UserFull;
import com.vk.api.sdk.objects.users.responses.SearchResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.ulearnstatistic.model.Sex;
import org.ulearnstatistic.model.Student;
import org.ulearnstatistic.model.University;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class VkRepository {

    /**
     * Получает информацию по студентам из ВК. В качестве критерия поиска служит список групп.
     * Поиск происходит по подписчикам переданных групп.
     * @param students Список моделей студентов, данные о которых надо найти
     * @param groups Список групп, в которых будет происходить поиск
     * @return Словарь, где ключом является имя студента, а значением полученные страницы (TODO переработать систему с однофамильцами?)
     */
    public static HashMap<String, SearchResponse> getStudentDataFromVk(List<Student> students, long[] groups) {
        var vk = new VkService(); // TODO добавить кнопку "дополнить данные", и показывать сколько еще осталось человек
        var studentDct = VkRequest.searchStudentsByGroups(vk,students,groups); // TODO получать айдишники по названию

        for (var key : studentDct.keySet()) {
            System.out.println(key + "\t" + studentDct.get(key));
        }

        return studentDct;
    }

    /**
     * Заносит полученную информацию о студентах в модели студентов.
     * Если по студенту было найдено несколько страниц, тогда используются веса для определения наиболее релевантой страницы,
     * такие как Университет и Институт (приоритет отдается студентам ИРИТ-РТФ и ФИИТ МатМех УрФУ),
     * Направление подготовки (приоритет отдается студентам Программной инженерии, Прикладной информатики и ИВТ),
     * а также тем студентам, которые начали учиться после 2018 года.
     * @param studentDct Словарь, где ключом является имя студента, а значением полученные страницы (TODO переработать систему с однофамильцами?)
     * @param students Модели студентов, в которые будут заноситься данные
     */
    public static void setStudentData(HashMap<String, SearchResponse> studentDct, List<Student> students) {
        var studentData = getWeightListsForStudentDatas(studentDct);
        for (var student : students) {
            var temp = studentData.get(student.getName());
            if (!temp.isEmpty()) {
                var data = temp.stream().findFirst().get().getValue();
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

    // TODO убрать String поставить id
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

    /**
     * Метод для быстрого сохранения данных о студентах в файл.
     * БОЛЕЕ НЕ ИСПОЛЬЗУЕТСЯ
     * @param studentDct Словарь, где ключом является имя студента, а значением полученные страницы (TODO переработать систему с однофамильцами?)
     * @param path Путь нового файла
     * @throws IOException
     */
    public static void serializeStudentData(HashMap<String,SearchResponse> studentDct, String path) throws IOException {
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

    /**
     * Метод для быстрого прочтения из файла сохраненных данных о студентах.
     * БОЛЕЕ НЕ ИСПОЛЬЗУЕТСЯ
     * @param path Путь до файла
     * @return Словарь, где ключом является имя студента, а значением полученные страницы (TODO переработать систему с однофамильцами?)
     * @throws IOException
     */
    public static HashMap<String,SearchResponse> deserializeStudentData(String path) throws IOException {
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
