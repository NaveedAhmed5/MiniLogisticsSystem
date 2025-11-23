package com.minilogistics.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class AdminDBConfig {
    static final String URL = "jdbc:mysql://localhost:3306/logistics_db";
    static final String USER = "root";
    static final String PASS = "";
    
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new SQLException("MySQL Driver not found in classpath");
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }
}