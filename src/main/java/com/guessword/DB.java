package com.guessword;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DB {

    private static final String URL;
    private static final String USERNAME;
    private static final String PASSWORD;

    static {
        try (InputStream input =
                     DB.class.getResourceAsStream("/db.properties")) {

            if (input == null) {
                throw new IllegalStateException(
                        "db.properties not found in src/main/resources");
            }

            Properties properties = new Properties();
            properties.load(input);

            URL = properties.getProperty("db.url");
            USERNAME = properties.getProperty("db.username");
            PASSWORD = properties.getProperty("db.password");

        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DB() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}
