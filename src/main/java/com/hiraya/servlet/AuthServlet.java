package com.hiraya.servlet;

import com.hiraya.dao.UserDAO;
import com.hiraya.model.User;
import com.hiraya.util.PasswordUtil;
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

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
    private UserDAO userDAO = new UserDAO();
    private Gson gson = new Gson();
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            sendError(response, "Invalid endpoint");
            return;
        }
        
        switch (pathInfo) {
            case "/register":
                handleRegister(request, response);
                break;
            case "/login":
                handleLogin(request, response);
                break;
            case "/logout":
                handleLogout(request, response);
                break;
            default:
                sendError(response, "Invalid endpoint");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        
        if ("/check".equals(pathInfo)) {
            checkAuth(request, response);
        } else {
            sendError(response, "Invalid endpoint");
        }
    }
    
    private void handleRegister(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
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
            
            String firstName = jsonData.get("firstName");
            String lastName = jsonData.get("lastName");
            String email = jsonData.get("email");
            String phone = jsonData.get("phone");
            String password = jsonData.get("password");
            
            // Validate input
            if (firstName == null || firstName.trim().isEmpty() ||
                lastName == null || lastName.trim().isEmpty() ||
                email == null || email.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
                
                sendError(response, "All fields are required");
                return;
            }
            
            // Check if user already exists
            User existingUser = userDAO.getUserByEmail(email);
            if (existingUser != null) {
                sendError(response, "Email already registered");
                return;
            }
            
            // Create new user
            User user = new User(firstName, lastName, email, phone, password);
            User registeredUser = userDAO.register(user, password);
            
            if (registeredUser != null) {
                // Create session
                HttpSession session = request.getSession();
                session.setAttribute("userId", registeredUser.getId());
                session.setAttribute("userEmail", registeredUser.getEmail());
                session.setAttribute("userName", registeredUser.getFullName());
                session.setAttribute("userType", registeredUser.getUserType());
                
                result.put("success", true);
                result.put("message", "Registration successful");
                result.put("userId", registeredUser.getId());
                result.put("userName", registeredUser.getFullName());
                result.put("email", registeredUser.getEmail());
                result.put("userType", registeredUser.getUserType());
            } else {
                sendError(response, "Registration failed. Please try again.");
                return;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            sendError(response, "An error occurred: " + e.getMessage());
            return;
        }
        
        response.getWriter().write(gson.toJson(result));
    }
    
    private void handleLogin(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
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
                
                sendError(response, "Email and password are required");
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
                
                // Check if user has an application (optional)
                // You can add ApplicationDAO here if needed
                
                result.put("success", true);
                result.put("message", "Login successful");
                result.put("userId", user.getId());
                result.put("userName", user.getFullName());
                result.put("email", user.getEmail());
                result.put("userType", user.getUserType());
            } else {
                sendError(response, "Invalid email or password");
                return;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            sendError(response, "An error occurred: " + e.getMessage());
            return;
        }
        
        response.getWriter().write(gson.toJson(result));
    }
    
    private void handleLogout(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        Map<String, Object> result = new HashMap<>();
        
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        
        result.put("success", true);
        result.put("message", "Logout successful");
        
        response.getWriter().write(gson.toJson(result));
    }
    
    private void checkAuth(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        Map<String, Object> result = new HashMap<>();
        HttpSession session = request.getSession(false);
        
        if (session != null && session.getAttribute("userId") != null) {
            int userId = (int) session.getAttribute("userId");
            User user = userDAO.getUserById(userId);
            
            if (user != null) {
                result.put("authenticated", true);
                result.put("userId", user.getId());
                result.put("userName", user.getFullName());
                result.put("email", user.getEmail());
                result.put("userType", user.getUserType());
            } else {
                result.put("authenticated", false);
            }
        } else {
            result.put("authenticated", false);
        }
        
        response.getWriter().write(gson.toJson(result));
    }
    
    private void sendError(HttpServletResponse response, String message) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        
        response.getWriter().write(gson.toJson(error));
    }
}