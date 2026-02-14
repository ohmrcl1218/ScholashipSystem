package com.hiraya.servlet;

import com.hiraya.dao.AdminDAO;
import com.hiraya.model.Admin;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/admin/login")
public class AdminLoginServlet extends HttpServlet {
    private AdminDAO adminDAO = new AdminDAO();
    private Gson gson = new Gson();
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Set CORS headers - IMPORTANT: Allow credentials
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        PrintWriter out = response.getWriter();
        
        try {
            System.out.println("\n===== ADMIN LOGIN ATTEMPT =====");
            
            // Read JSON from request body
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            String jsonBody = sb.toString();
            System.out.println("Request body: " + jsonBody);
            
            Map<String, String> jsonData = gson.fromJson(jsonBody, Map.class);
            String email = jsonData.get("email");
            String password = jsonData.get("password");
            
            System.out.println("Email extracted: '" + email + "'");
            
            // Validate input
            if (email == null || email.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Email is required");
                out.write(gson.toJson(result));
                return;
            }
            
            if (password == null || password.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Password is required");
                out.write(gson.toJson(result));
                return;
            }
            
            // Authenticate admin
            Admin admin = adminDAO.login(email.trim(), password);
            
            if (admin != null) {
                // CREATE SESSION - THIS IS CRITICAL
                HttpSession session = request.getSession(true);
                session.setAttribute("adminId", admin.getId());
                session.setAttribute("adminEmail", admin.getEmail());
                session.setAttribute("adminName", admin.getFullName());
                session.setAttribute("adminRole", admin.getRole());
                session.setAttribute("userType", "admin");
                
                // Set session timeout (optional)
                session.setMaxInactiveInterval(3600); // 1 hour
                
                System.out.println("✅ Session created with ID: " + session.getId());
                System.out.println("Session ID in cookie will be: " + session.getId());
                
                result.put("success", true);
                result.put("message", "Login successful");
                result.put("adminId", admin.getId());
                result.put("adminName", admin.getFullName());
                result.put("adminEmail", admin.getEmail());
                result.put("adminRole", admin.getRole());
            } else {
                System.err.println("❌ Admin login failed - invalid credentials");
                result.put("success", false);
                result.put("message", "Invalid email or password");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Server error: " + e.getMessage());
        }
        
        out.write(gson.toJson(result));
        out.flush();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Set CORS headers
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        PrintWriter out = response.getWriter();
        
        try {
            HttpSession session = request.getSession(false);
            
            if (session != null && session.getAttribute("adminId") != null) {
                int adminId = (int) session.getAttribute("adminId");
                Admin admin = adminDAO.getAdminById(adminId);
                
                if (admin != null) {
                    result.put("authenticated", true);
                    result.put("adminId", admin.getId());
                    result.put("adminName", admin.getFullName());
                    result.put("adminEmail", admin.getEmail());
                    result.put("adminRole", admin.getRole());
                } else {
                    result.put("authenticated", false);
                }
            } else {
                result.put("authenticated", false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("authenticated", false);
            result.put("error", e.getMessage());
        }
        
        out.write(gson.toJson(result));
        out.flush();
    }
    
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setStatus(HttpServletResponse.SC_OK);
    }
}