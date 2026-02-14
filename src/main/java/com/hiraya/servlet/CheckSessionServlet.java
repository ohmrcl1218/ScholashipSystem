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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/admin/check-session")
public class CheckSessionServlet extends HttpServlet {
    private AdminDAO adminDAO = new AdminDAO();
    private Gson gson = new Gson();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        
        Map<String, Object> result = new HashMap<>();
        HttpSession session = request.getSession(false);
        
        try {
            if (session != null && session.getAttribute("adminId") != null) {
                int adminId = (int) session.getAttribute("adminId");
                Admin admin = adminDAO.getAdminById(adminId);
                
                if (admin != null) {
                    Map<String, Object> adminInfo = new HashMap<>();
                    adminInfo.put("id", admin.getId());
                    adminInfo.put("fullName", admin.getFullName());
                    adminInfo.put("email", admin.getEmail());
                    adminInfo.put("role", admin.getRole());
                    adminInfo.put("roleDisplayName", admin.getRoleDisplayName());
                    adminInfo.put("isAdministrator", admin.isScholarshipAdministrator());
                    adminInfo.put("isReviewer", admin.isReviewer());
                    
                    result.put("authenticated", true);
                    result.put("admin", adminInfo);
                    result.put("userType", "admin");
                    result.put("hasSession", true);
                } else {
                    session.invalidate();
                    result.put("authenticated", false);
                    result.put("hasSession", false);
                }
            } else {
                result.put("authenticated", false);
                result.put("hasSession", false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("authenticated", false);
            result.put("hasSession", false);
            result.put("error", e.getMessage());
        }
        
        response.getWriter().write(gson.toJson(result));
    }
    
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setStatus(HttpServletResponse.SC_OK);
    }
}