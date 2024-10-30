package org.ulearnstatistic;

public class Student {
    private final int id;
    private String surname;
    private String name;
    private Sex sex;
    private int age;
    private String group;
    private String country;
    private String city;

    public Student() {
        id = hashCode(); // ??
    }
}
