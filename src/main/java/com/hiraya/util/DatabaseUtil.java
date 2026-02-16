package com.hiraya.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class DatabaseUtil {
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/hiraya_scholarship?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String JDBC_USER = "root";
    private static final String JDBC_PASSWORD = "Wsqk@2jej76"; 
    
    private static DatabaseUtil instance;
    
    private DatabaseUtil() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ MySQL JDBC Driver loaded");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL JDBC Driver not found!");
            e.printStackTrace();
        }
    }
    
    public static DatabaseUtil getInstance() {
        if (instance == null) {
            instance = new DatabaseUtil();
        }
        return instance;
    }
    
    /**
     * CRITICAL FIX: Create a NEW connection each time
     * DO NOT reuse a single connection
     */
    public Connection getConnection() throws SQLException {
        try {
            System.out.println("📡 Creating new database connection...");
            long startTime = System.currentTimeMillis();
            
            // Create a brand new connection - don't reuse
            Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
            
            long endTime = System.currentTimeMillis();
            System.out.println("✅ New connection created in " + (endTime - startTime) + "ms");
            System.out.println("   Database: " + conn.getCatalog());
            System.out.println("   Connection ID: " + conn.hashCode()); // Show unique connection ID
            
            return conn;
        } catch (SQLException e) {
            System.err.println("\n❌❌❌ DATABASE CONNECTION FAILED ❌❌❌");
            System.err.println("   Error Code: " + e.getErrorCode());
            System.err.println("   SQL State: " + e.getSQLState());
            System.err.println("   Message: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Helper method to close connection - should be called in finally block
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
                System.out.println("🔒 Connection closed: " + conn.hashCode());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static Timestamp getCurrentTimestamp() {
        return Timestamp.valueOf(LocalDateTime.now());
    }
}