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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/register")
public class RegisterServlet extends HttpServlet {
    private UserDAO userDAO = new UserDAO();
    private Gson gson = new Gson();
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        PrintWriter out = response.getWriter();
        
        try {
            // Read JSON from request body
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            String jsonBody = sb.toString();
            System.out.println("===== REGISTRATION ATTEMPT =====");
            System.out.println("Received JSON: " + jsonBody);
            
            Map<String, String> jsonData = gson.fromJson(jsonBody, Map.class);
            
            String firstName = jsonData.get("firstName");
            String lastName = jsonData.get("lastName");
            String email = jsonData.get("email");
            String phone = jsonData.get("phone");
            String password = jsonData.get("password");
            
            System.out.println("First Name: " + firstName);
            System.out.println("Last Name: " + lastName);
            System.out.println("Email: " + email);
            System.out.println("Phone: " + phone);
            System.out.println("Password length: " + (password != null ? password.length() : 0));
            
            // Validate input
            if (firstName == null || firstName.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "First name is required");
                out.write(gson.toJson(result));
                return;
            }
            
            if (lastName == null || lastName.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Last name is required");
                out.write(gson.toJson(result));
                return;
            }
            
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
            
            // Check if user already exists
            System.out.println("Checking if user exists: " + email);
            User existingUser = null;
            try {
                existingUser = userDAO.getUserByEmail(email);
            } catch (Exception e) {
                System.err.println("Error checking existing user: " + e.getMessage());
                e.printStackTrace();
                result.put("success", false);
                result.put("message", "Database error while checking email: " + e.getMessage());
                out.write(gson.toJson(result));
                return;
            }
            
            if (existingUser != null) {
                System.out.println("User already exists: " + email);
                result.put("success", false);
                result.put("message", "Email already registered");
                out.write(gson.toJson(result));
                return;
            }
            
            // Create new user
            System.out.println("Creating new user...");
            User user = new User(firstName, lastName, email, phone, password);
            
            User registeredUser = null;
            try {
                registeredUser = userDAO.register(user, password);
            } catch (Exception e) {
                System.err.println("Exception during registration: " + e.getMessage());
                e.printStackTrace();
                
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                
                result.put("success", false);
                result.put("message", "Database error: " + e.getMessage());
                result.put("exception", e.getClass().getName());
                result.put("stacktrace", sw.toString());
                out.write(gson.toJson(result));
                return;
            }
            
            if (registeredUser != null) {
                System.out.println("Registration successful! User ID: " + registeredUser.getId());
                
                // Create session
                HttpSession session = request.getSession();
                session.setAttribute("userId", registeredUser.getId());
                session.setAttribute("userEmail", registeredUser.getEmail());
                session.setAttribute("userName", registeredUser.getFullName());
                
                result.put("success", true);
                result.put("message", "Registration successful");
                result.put("userId", registeredUser.getId());
                result.put("userName", registeredUser.getFullName());
                result.put("email", registeredUser.getEmail());
            } else {
                System.err.println("Registration failed - userDAO.register returned null");
                result.put("success", false);
                result.put("message", "Registration failed. Please try again.");
            }
            
        } catch (Exception e) {
            System.err.println("Unexpected error in RegisterServlet:");
            e.printStackTrace();
            
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            
            result.put("success", false);
            result.put("message", "Server error: " + e.getMessage());
            result.put("exception", e.getClass().getName());
            result.put("stacktrace", sw.toString());
        }
        
        System.out.println("Sending response: " + gson.toJson(result));
        System.out.println("===== END REGISTRATION ATTEMPT =====\n");
        
        out.write(gson.toJson(result));
    }
}