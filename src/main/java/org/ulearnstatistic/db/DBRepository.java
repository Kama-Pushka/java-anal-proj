package org.ulearnstatistic.db;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// может работать с классами, которые имеют только примитивные поля
public class DBRepository {
    private static Connection conn;
    private static String URL = "jdbc:sqlite:data/ulearn-data.db";

    public static void UpdateURL(String URL) {
        DBRepository.URL = URL;
    }

    public static void connect() {
        try {
            conn = DriverManager.getConnection(URL);
            System.out.println("Connected to database.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public static void createTable(String sqlQuery) {
        try(var conn = DriverManager.getConnection(URL);
            var stmt = conn.createStatement()) {
           stmt.execute(sqlQuery);
           System.out.println("Table created.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // первое по порядку поле дожно быть ключевым
    public static String getCreateTableQuery(Class<?> cls, String[] primaryKeys, Triple<String, String, String>[] foreignKeys) {
        var sql = new StringBuilder("CREATE TABLE IF NOT EXISTS %s(\n".formatted(cls.getSimpleName()));
        var fields = cls.getDeclaredFields();
        if (fields.length == 0) throw new IllegalArgumentException("No fields in class " + cls.getName());

        for (var field : fields) {
            sql.append("\"%s\" %s NOT NULL,\n".formatted(field.getName(), getSqlType(field.getType())));
        }
        if (primaryKeys != null && primaryKeys.length > 0)
            sql.append("PRIMARY KEY (%s),\n".formatted(String.join(",", primaryKeys)));
        if (foreignKeys != null) for (var key : foreignKeys)
            sql.append("FOREIGN KEY (%s) REFERENCES %s(%s),\n".formatted(key.getLeft(), key.getMiddle(), key.getRight()));
        sql.deleteCharAt(sql.length() - 1);
        sql.deleteCharAt(sql.length() - 1);
        sql.append(")");
        return sql.toString();
    }

    private static String getSqlType(Class<?> type) {
        if (type.equals(int.class) || type.equals(Integer.class)) {
            return "INTEGER";
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            return "BIGINT";
        } else if (type.equals(java.sql.Date.class)) {
            return "DATE";
        }
        return "VARCHAR(255)";
    }

    public static Pair<String, String[]> getSaveIntoTableQuery(Class<?> cls) {
        var fields = cls.getDeclaredFields();
        if (fields.length == 0) throw new IllegalArgumentException("No fields in class " + cls.getName());

        var columns = new StringBuilder();
        var values = new StringBuilder();
        for (var field : fields) {
            columns.append("\"").append(field.getName()).append("\"").append(",");
            values.append("?").append(",");
        }
        columns.deleteCharAt(columns.length() - 1);
        values.deleteCharAt(values.length() - 1);
        return Pair.of("INSERT INTO %s(%s) VALUES(%s)\n".formatted(cls.getSimpleName(), columns.toString(), values.toString()),
                columns.toString().replace("\"","").split(","));
    }

    // колонки должны идти в том же порядке, в котором указаны в sql запросе
    public static void saveIntoTable(String sqlQuery, List<?> datas, String[] columns) {
        for (var data : datas) {
            try(var conn = DriverManager.getConnection(URL);
                var stmt = conn.prepareStatement(sqlQuery)) {
                for (var i = 0; i < columns.length; i++) {
                    var field = data.getClass().getField(columns[i]).get(data);
                    field = field == null ? "null" : field.toString();
                    stmt.setObject(i+1, field);
                }
                stmt.executeUpdate();
                System.out.println("Table updated.");
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Pair<String, String[]> getDataFromTableQuery(Class<?> cls) {
        var fields = cls.getDeclaredFields();
        if (fields.length == 0) throw new IllegalArgumentException("No fields in class " + cls.getName());

        var columns = new StringBuilder();
        for (var field : fields) {
            columns.append("\"").append(field.getName()).append("\"").append(",");
        }
        columns.deleteCharAt(columns.length() - 1);
        return Pair.of("SELECT %s\nFROM %s".formatted(columns.toString(), cls.getSimpleName()),
                columns.toString().replace("\"","").split(","));
    }

    public static List<?> getDataFromTable(String sqlQuery, Class<?> cls, String[] columns) {
        try(var conn = DriverManager.getConnection(URL);
            var stmt = conn.createStatement();
            var result = stmt.executeQuery(sqlQuery)) {
            var objs = new ArrayList<>();

            while (result.next()) {
                var obj = cls.getConstructor().newInstance();
                var fields = obj.getClass().getDeclaredFields();
                if (fields.length == 0) throw new IllegalArgumentException("No fields in class " + cls.getName());

                for (var column : columns) {
                    var field = obj.getClass().getField(column);
                    var res = result.getObject(column);
                    if (res.equals("null")) continue;
                    field.set(obj, res);
                }
                objs.add(obj);
            }
            return objs;
        } catch (SQLException | InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
