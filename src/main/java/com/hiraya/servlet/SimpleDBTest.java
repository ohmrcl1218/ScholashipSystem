package com.hiraya.servlet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SimpleDBTest {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/hiraya_scholarship?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        String user = "root";
        String password = "Wsqk@2jej76"; // <--- PUT YOUR PASSWORD HERE
        
        System.out.println("=== DATABASE CONNECTION TEST ===");
        
        // Test 1: Check if driver exists
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✓ MySQL JDBC Driver found");
        } catch (ClassNotFoundException e) {
            System.out.println("✗ MySQL JDBC Driver NOT found!");
            System.out.println("  Fix: Add mysql-connector-java.jar to classpath");
            return;
        }
        
        // Test 2: Try to connect
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("✓ Connected to database!");
            System.out.println("  Database: " + conn.getCatalog());
            System.out.println("  Driver: " + conn.getMetaData().getDriverName());
            System.out.println("\n✅ SUCCESS! Database is working!");
            
        } catch (SQLException e) {
            System.out.println("✗ Connection FAILED!");
            System.out.println("  Error Code: " + e.getErrorCode());
            System.out.println("  SQL State: " + e.getSQLState());
            System.out.println("  Message: " + e.getMessage());
            
            // Helpful messages
            if (e.getErrorCode() == 0) {
                System.out.println("\n🔴 FIX: MySQL is not running!");
                System.out.println("  1. Press Windows + R");
                System.out.println("  2. Type 'services.msc'");
                System.out.println("  3. Find 'MySQL80'");
                System.out.println("  4. Right-click → Start");
            } else if (e.getErrorCode() == 1045) {
                System.out.println("\n🔴 FIX: Wrong MySQL password!");
                System.out.println("  1. Open DatabaseConfig.java");
                System.out.println("  2. Change PASSWORD variable");
                System.out.println("  3. Current password: '" + password + "'");
            } else if (e.getErrorCode() == 1049) {
                System.out.println("\n🔴 FIX: Database does not exist!");
                System.out.println("  1. Open MySQL Workbench");
                System.out.println("  2. Run: CREATE DATABASE hiraya_scholarship;");
            }
        }
    }
}