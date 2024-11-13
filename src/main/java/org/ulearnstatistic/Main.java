package org.ulearnstatistic;

import java.io.*;

import org.ulearnstatistic.parser.CSVUlearnReader;

public class Main {
    public static void main(String[] args) {
        try {
            var ulearn = new CSVUlearnReader();
            ulearn.read("data\\java-rtf.csv");

            var moduleList = ulearn.getModules();
            var students = ulearn.getStudents();

            ulearn.write("data\\report.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

