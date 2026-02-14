package com.hiraya.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;

@WebServlet("/api/admin/logout")
public class AdminLogoutServlet extends HttpServlet {
    private Gson gson = new Gson();
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        Map<String, Object> result = new HashMap<>();
        
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // Destroy session
        }
        
        result.put("success", true);
        result.put("message", "Logged out successfully");
        
        response.getWriter().write(gson.toJson(result));
    }
}