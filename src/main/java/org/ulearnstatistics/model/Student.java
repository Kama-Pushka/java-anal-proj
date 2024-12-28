package org.ulearnstatistics.model;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Objects;

import org.ulearnstatistics.db.model.StudentEntity;

public class Student {
    private final int id;
    private String surname;
    private String name;
    private Sex sex;
    private Calendar dateOfBirth;
    private int age;
    private String group; // TODO в отдельный класс?
    private String country;
    private String city;
    private University university;

    public Student(int id) {
        this.id = id;
    }

    public Student(StudentEntity student) {
        this.id = student.id;
        this.surname = student.surname;
        this.name = student.name;
        this.sex = Objects.equals(student.sex, "null") ? Sex.valueOf(student.sex) : null; // TODO затычка
        this.dateOfBirth = Calendar.getInstance();
        dateOfBirth.setTime(student.getDateOfBirth()); // TODO а работает?
        this.group = student.group;
        this.country = student.country;
        this.city = student.city;
        this.university = new University(447, student.university); // TODO ??
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
    public void setGroup(String group) {
        this.group = group;
    }
    public void setCountry(String country) {
        this.country = country;
    }
    public void setCity(String city) {
        this.city = city;
    }
    public void setUniversity(University university) {
        this.university = university;
    }
    public void setDateOfBirth(int day, int month, int year) {
        this.dateOfBirth = Calendar.getInstance();
        dateOfBirth.set(day,month,year);
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
    public Calendar getDateOfBirth() {
        return dateOfBirth;
    }
    public int getAge() {
        if (dateOfBirth == null) return 0;

        if (dateOfBirth.get(Calendar.YEAR) == 0) return 0; // TODO
        return LocalDate.now().getYear() - dateOfBirth.get(Calendar.YEAR); // TODO
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
    public University getUniversity() {
        return university;
    }

    @Override
    public String toString() {
        return "%s".formatted(name); // TODO парсинг для surname
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return id == student.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
