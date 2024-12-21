package org.ulearnstatistic.db;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

/**
 * Предупреждение: Данный класс работы с базой данных может обрабатывать только с пользовательские классы,
 * которые имеют ТОЛЬКО примитивные поля. Если в классе существуют иные непримитивные поля (даже private), их необходимо исключить,
 * либо создать их отдельный класс для работы с БД.
 */
public class DBService {
    private static Connection conn;
    private static String URL = "jdbc:sqlite:data/ulearn-data.db";

    // а нужно ли простаскивать этот метод?
    public static void updateURL(String URL) {
        DBService.URL = "jdbc:sqlite:" + URL;
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

    /**
     * Получить информацию из таблицы по средствам SELECT SQL запроса
     * @param sql SQL-запрос, основанный на операторе SELECT
     * @return Список строк полученной информации. Отдельные атрибуты разделены знаком "||"
     */
    public static List<String> getDataFromTable(String sql) {
        try(var conn = DriverManager.getConnection(URL);
            var stmt = conn.createStatement();
            var result = stmt.executeQuery(sql)) {

            var list = new ArrayList<String>();
            var colNums = result.getMetaData().getColumnCount() + 1;
            while (result.next()) {
                var str = new StringBuilder(result.getString(1));
                for (var i = 2; i < colNums; i++)
                    str.append("||").append(result.getString(i));
                list.add(str.toString());
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Создание таблицы.
     * @param sqlQuery SQL-запрос состоящий из запроса на создание таблицы
     */
	public static void createTable(String sqlQuery) {
        try(var conn = DriverManager.getConnection(URL);
            var stmt = conn.createStatement()) {
           stmt.execute(sqlQuery);
           System.out.println("Table created.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Формирование SQL запроса для создания таблицы по всем полям класса (в т.ч. private)
     * @param cls Класс, поля которого станут колонками
     * @param primaryKeys Ключевые атрибуты - названия полей класса
     * @param foreignKeys Внешние ключи - название полей класса, таблицы, с которой создается связь
     *                    и название колонок той таблицы, на которые создаются ссылки
     * @return SQL-запрос
     */
    public static String getCreateTableQuery(Class<?> cls, String[] primaryKeys, Triple<String, String, String>[] foreignKeys) {
        var sql = new StringBuilder("CREATE TABLE IF NOT EXISTS %s(\n".formatted(cls.getSimpleName()));
        var fields = cls.getDeclaredFields();
        if (fields.length == 0) throw new IllegalArgumentException("No fields in class " + cls.getName());

        for (var field : fields) {
            sql.append("\"%s\" %s NOT NULL,\n".formatted(field.getName(), getSqlType(field.getType()))); // TODO убрать NOT NULL
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

    /**
     * Формирование запроса на сохранение всех полей модели в таблицу
     * @param cls Класс, поля которого будут сохранены
     * @return SQL-запрос и список колонок, которые будут сохранены в таблицу
     */
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

    /**
     * Сохранить информацию из моделей в таблицу.
     * @param sqlQuery SQL-запрос на добавление данных в таблицу.
     * @param datas Модели, которые необходимо сохранить
     * @param columns Колонки, которые должны быть сохранены.
     *                Колонки должны совпадать с колонками, указанными в SQL-запросе
     */
    public static void saveIntoTable(String sqlQuery, List<?> datas, String[] columns) {
        try(var conn = DriverManager.getConnection(URL);
            var stmt = conn.prepareStatement(sqlQuery)) {
            var j = 0;
            conn.setAutoCommit(false);
            for (var data : datas) {
                for (var i = 0; i < columns.length; i++) {
                    var field = data.getClass().getField(columns[i]).get(data);
                    field = field == null ? "null" : field.toString();
                    stmt.setObject(i+1, field);
                }
                stmt.addBatch();
                System.out.println("Table is being prepared to update.");
                j++;

                if (j % 1000 == 0 || j == datas.size()) {
                    stmt.executeBatch();
                    conn.commit();
                    System.out.println("Table FINALLY updated.");
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Формирование SQL-запрос на получение всех данных из таблицы.
     * @param cls Класс, информацию о котором хотим получить из соотвествующей таблицы
     * @return SQL-запрос и колонки, в которые будут записываться данные
     */
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

    /**
     * Получение всех данных из таблицы
     * @param sqlQuery SQL-запрос на получение данных из таблицы
     * @param cls Класс, в который будем сохранять полученные данные
     * @param columns Колонки, в которые будут сохраняться полученные данные.
     *                Колонки должны совпадать с колонками, указанными в SQL-запросе
     * @return
     */
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

    /**
     * НЕ ИСПОЛЬЗУЕТСЯ
     */
	public static List<?> getDataFromTableOLD(String sqlQuery, Class<?> cls, String[] columns) {
        try(var conn = DriverManager.getConnection(URL);
            var stmt = conn.createStatement();
            var result = stmt.executeQuery(sqlQuery)) {
            var objs = new ArrayList<>();

            while (result.next()) {
                var obj = cls.getConstructor().newInstance();
                var fields = obj.getClass().getDeclaredFields();
                if (fields.length == 0) throw new IllegalArgumentException("No fields in class " + cls.getName());

                for (var field : columns) {
                    var test = "set%s".formatted(field.substring(0, 1).toUpperCase() + field.substring(1));
                    var setMethod = Arrays.stream(obj.getClass().getDeclaredMethods()).filter(m -> m.getName().equals(test)).findFirst().get();
                    //var type = Arrays.stream(setMethod.getParameterTypes()).findFirst();
                    var test1 = result.getObject(field);
                    if (test1.equals("null")) continue;
                    setMethod.invoke(obj, test1);
                }
                objs.add(obj);
            }
            return objs;
        } catch (SQLException | InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
