package com.hiraya.servlet;

import com.hiraya.util.PasswordUtil;
import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/generate-hash")
public class GenerateHashServlet extends HttpServlet {
    private Gson gson = new Gson();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        PrintWriter out = response.getWriter();
        
        try {
            String password = request.getParameter("password");
            if (password == null || password.isEmpty()) {
                password = "admin123"; // default
            }
            
            String hash = PasswordUtil.hashPassword(password);
            
            result.put("success", true);
            result.put("password", password);
            result.put("hash", hash);
            result.put("sql", "INSERT INTO users (password_hash) VALUES ('" + hash + "');");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        out.write(gson.toJson(result));
    }
}