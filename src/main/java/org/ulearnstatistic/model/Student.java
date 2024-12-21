package org.ulearnstatistic.model;

public class Student {
    private final int id;
    private String surname;
    private String name;
    private Sex sex;
    private int age;
    private String group; // TODO в отдельный класс?
    private String country;
    private String city;

    public Student() {
        id = hashCode(); // ??
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setSurname(String surname) {
        this.surname = surname;
    }
    public void setSex(Sex sex) {
        this.sex = sex;
    }
    public void setAge(int age) {
        this.age = age;
    }
    public void setGroup(String group) {
        this.group = group;
    }
    public void setCountry(String country) {
        this.country = country;
    }
    public void setCity(String city) {
        this.city = city;
    }

    public int getId() {
        return id;
    }
    public String getSurname() {
        return surname;
    }
    public String getName() {
        return name;
    }
    public Sex getSex() {
        return sex;
    }
    public int getAge() {
        return age;
    }
    public String getGroup() {
        return group;
    }
    public String getCountry() {
        return country;
    }
    public String getCity() {
        return city;
    }

    @Override
    public String toString() {
        return "%s".formatted(name); // TODO парсинг для surname
    }
}
