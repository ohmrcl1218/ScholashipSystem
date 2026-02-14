package com.hiraya.servlet;

import com.hiraya.dao.ApplicationDAO;
import com.hiraya.dao.UserDAO;
import com.hiraya.model.Application;
import com.hiraya.model.User;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {
    private UserDAO userDAO = new UserDAO();
    private ApplicationDAO applicationDAO = new ApplicationDAO();
    private Gson gson = new Gson();
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Read JSON from request body
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            Map<String, String> jsonData = gson.fromJson(sb.toString(), Map.class);
            
            String email = jsonData.get("email");
            String password = jsonData.get("password");
            
            // Validate input
            if (email == null || email.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
                
                result.put("success", false);
                result.put("message", "Email and password are required");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
            // Authenticate user
            User user = userDAO.login(email, password);
            
            if (user != null) {
                // Create session
                HttpSession session = request.getSession();
                session.setAttribute("userId", user.getId());
                session.setAttribute("userEmail", user.getEmail());
                session.setAttribute("userName", user.getFullName());
                session.setAttribute("userType", user.getUserType());
                
                // Check if user has an application
                Application application = applicationDAO.getApplicationByUserId(user.getId());
                boolean hasApplication = application != null;
                boolean isSubmitted = hasApplication && "submitted".equals(application.getApplicationStatus());
                
                result.put("success", true);
                result.put("message", "Login successful");
                result.put("userId", user.getId());
                result.put("userName", user.getFullName());
                result.put("email", user.getEmail());
                result.put("userType", user.getUserType());
                result.put("hasApplication", hasApplication);
                result.put("isSubmitted", isSubmitted);
                
                if (hasApplication) {
                    result.put("applicationId", application.getId());
                    result.put("applicationStatus", application.getApplicationStatus());
                    result.put("referenceNumber", application.getReferenceNumber());
                }
            } else {
                result.put("success", false);
                result.put("message", "Invalid email or password");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "An error occurred: " + e.getMessage());
        }
        
        response.getWriter().write(gson.toJson(result));
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        HttpSession session = request.getSession(false);
        
        if (session != null && session.getAttribute("userId") != null) {
            int userId = (int) session.getAttribute("userId");
            User user = userDAO.getUserById(userId);
            
            if (user != null) {
                Application application = applicationDAO.getApplicationByUserId(userId);
                boolean hasApplication = application != null;
                boolean isSubmitted = hasApplication && "submitted".equals(application.getApplicationStatus());
                
                result.put("authenticated", true);
                result.put("userId", user.getId());
                result.put("userName", user.getFullName());
                result.put("email", user.getEmail());
                result.put("userType", user.getUserType());
                result.put("hasApplication", hasApplication);
                result.put("isSubmitted", isSubmitted);
                
                if (hasApplication) {
                    result.put("applicationId", application.getId());
                    result.put("applicationStatus", application.getApplicationStatus());
                    result.put("referenceNumber", application.getReferenceNumber());
                }
            } else {
                result.put("authenticated", false);
            }
        } else {
            result.put("authenticated", false);
        }
        
        response.getWriter().write(gson.toJson(result));
    }
}