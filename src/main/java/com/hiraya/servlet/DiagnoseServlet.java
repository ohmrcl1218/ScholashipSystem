package com.hiraya.servlet;

import com.hiraya.config.DatabaseConfig;
import com.hiraya.dao.UserDAO;
import com.hiraya.model.User;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/diagnose")
public class DiagnoseServlet extends HttpServlet {
    private Gson gson = new Gson();
    private UserDAO userDAO = new UserDAO();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> dbCheck = new HashMap<>();
        Map<String, Object> tableCheck = new HashMap<>();
        PrintWriter out = response.getWriter();
        
        try {
            // 1. Test Database Connection
            try {
                Connection conn = DatabaseConfig.getConnection();
                dbCheck.put("connection", "✅ SUCCESS");
                dbCheck.put("database", conn.getCatalog());
                
                // 2. Check if users table exists
                DatabaseMetaData meta = conn.getMetaData();
                ResultSet tables = meta.getTables(null, null, "users", null);
                if (tables.next()) {
                    tableCheck.put("exists", "✅ YES");
                    
                    // 3. Check table structure
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("DESCRIBE users");
                    Map<String, String> columns = new HashMap<>();
                    while (rs.next()) {
                        columns.put(rs.getString("Field"), rs.getString("Type"));
                    }
                    tableCheck.put("columns", columns);
                    
                    // 4. Check row count
                    rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users");
                    if (rs.next()) {
                        tableCheck.put("row_count", rs.getInt("count"));
                    }
                    rs.close();
                    stmt.close();
                } else {
                    tableCheck.put("exists", "❌ NO - Table 'users' does not exist");
                }
                tables.close();
                conn.close();
                
            } catch (Exception e) {
                dbCheck.put("connection", "❌ FAILED");
                dbCheck.put("error", e.getMessage());
            }
            
            result.put("database", dbCheck);
            result.put("users_table", tableCheck);
            
            // 5. Test registration
            try {
                String testEmail = "diagnose_" + System.currentTimeMillis() + "@test.com";
                User testUser = new User("Diagnose", "Test", testEmail, "1234567890", "Test123!");
                User registered = userDAO.register(testUser, "Test123!");
                
                if (registered != null) {
                    result.put("test_registration", "✅ SUCCESS - User created with ID: " + registered.getId());
                    
                    // Clean up test user
                    // You might want to delete test users periodically
                } else {
                    result.put("test_registration", "❌ FAILED - Check console for SQL error");
                }
            } catch (Exception e) {
                result.put("test_registration", "❌ ERROR: " + e.getMessage());
            }
            
            result.put("success", true);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            e.printStackTrace();
        }
        
        out.write(gson.toJson(result));
    }
}