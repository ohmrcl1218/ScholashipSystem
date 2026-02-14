package com.hiraya.servlet;

import com.hiraya.util.DatabaseUtil;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/test-connection")
public class TestConnectionServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        
        System.out.println("\n🔍 TESTING DATABASE CONNECTION WITH PASSWORD");
        
        try {
            DatabaseUtil dbUtil = DatabaseUtil.getInstance();
            Connection conn = dbUtil.getConnection();
            
            result.put("success", true);
            result.put("message", "Database connected successfully!");
            result.put("database", conn.getCatalog());
            
            conn.close();
            
            System.out.println("✅ SUCCESS! Database connected with password");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Failed: " + e.getMessage());
            System.err.println("❌ FAILED: " + e.getMessage());
            e.printStackTrace();
        }
        
        out.write(gson.toJson(result));
    }
}