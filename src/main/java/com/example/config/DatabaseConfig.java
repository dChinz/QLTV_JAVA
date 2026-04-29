package com.example.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    // Sửa lại thông tin kết nối Oracle
    private static final String DB_URL = "jdbc:oracle:thin:@localhost:1521/orcl";
    private static final String DB_USER     = "chinh";
    private static final String DB_PASSWORD = "123";

    private static DatabaseConfig instance;
    private Connection connection;

    private DatabaseConfig() {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("[DB] Connected to Oracle successfully.");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Oracle JDBC Driver not found.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot connect to database: " + e.getMessage(), e);
        }
    }

    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot reconnect to database.", e);
        }
        return connection;
    }

    public static void closeConnection() {
        if (instance != null) {
            try {
                if (instance.connection != null && !instance.connection.isClosed()) {
                    instance.connection.close();
                    System.out.println("[DB] Connection closed.");
                }
            } catch (SQLException e) {
                System.err.println("[DB] Error closing connection: " + e.getMessage());
            }
        }
    }
}