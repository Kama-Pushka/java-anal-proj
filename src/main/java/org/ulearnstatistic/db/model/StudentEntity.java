package org.ulearnstatistic.db.model;

import org.ulearnstatistic.model.Sex;
import org.ulearnstatistic.model.Student;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class StudentEntity {
    public int id;
    public String surname;
    public String name;
    public String sex;
    public String dateOfBirth;
    public String group;
    public String country;
    public String city;
    public String university; // TODO ??

    public StudentEntity() {}

    public StudentEntity(Student student) {
        id = student.getId();
        surname = student.getSurname();
        name = student.getName();
        sex = student.getSex() != null ? student.getSex().name() : null; // TODO затычка
        var date = student.getDateOfBirth();
        dateOfBirth = date != null ? new Date(date.getTimeInMillis()).toString() : null;
        group = student.getGroup();
        country = student.getCountry();
        city = student.getCity();
        var univ = student.getUniversity();
        university = univ != null ? univ.Name() : null;
    }

    public Date getDateOfBirth() {
        try {
            var date = new SimpleDateFormat("dd/MM/yyyy").parse(dateOfBirth);
            return new Date(date.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
