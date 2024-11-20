package org.ulearnstatistic;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import com.google.gson.Gson;
import com.vk.api.sdk.objects.users.UserFull;
import com.vk.api.sdk.objects.users.responses.SearchResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.ulearnstatistic.parser.CSVUlearnReader;
import org.ulearnstatistic.vkApi.VkRepository;
import org.ulearnstatistic.vkApi.VkRequest;

public class Main {
    public static void main(String[] args) {
        try {
            var ulearn = new CSVUlearnReader();
            ulearn.read("data\\java-rtf.csv");

            var moduleList = ulearn.getModules();
            var students = ulearn.getStudents();

            var vk = new VkRepository();
            var groups = new long[] { 6214974, 195681601, 22941070 };
            var studentDct = VkRequest.searchStudentsByGroups(vk,students,groups);

            for (var key : studentDct.keySet()) {
                System.out.println(key + "\t" + studentDct.get(key));
            }

            serializeStudentData(studentDct,"data\\student_info.txt");

            ulearn.write("data\\report.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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

