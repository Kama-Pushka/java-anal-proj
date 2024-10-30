package org.ulearnstatistic;

import java.io.*;
import java.util.*;

import com.opencsv.*;
import com.opencsv.exceptions.CsvValidationException;

public class Main {
    public static void main(String[] args) {
        try {
            var ulearn = new CSVUlearnReader();
            ulearn.read("java-anal-proj\\data\\basicprogramming_2.csv"); // TODO relative path

            var moduleList = ulearn.getModules();
            var students = ulearn.getStudents();

            ulearn.write("java-anal-proj\\data\\report.txt"); // TODO relative path
        } catch (IOException | CsvValidationException e) {
            throw new RuntimeException(e);
        }
    }
}

