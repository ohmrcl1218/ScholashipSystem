package com.hiraya.servlet;

import com.hiraya.config.DatabaseConfig;
import com.hiraya.dao.AdminDAO;
import com.hiraya.dao.ApplicationDAO;
import com.hiraya.dao.DocumentDAO;
import com.hiraya.dao.UserDAO;
import com.hiraya.model.Admin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/admin/dashboard/*")
public class AdminDashboardServlet extends HttpServlet {
    private AdminDAO adminDAO;
    private ApplicationDAO applicationDAO;
    private DocumentDAO documentDAO;
    private UserDAO userDAO;
    private Gson gson;
    
    @Override
    public void init() {
        adminDAO = new AdminDAO();
        applicationDAO = new ApplicationDAO();
        documentDAO = new DocumentDAO();
        userDAO = new UserDAO();
        
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setDateFormat("yyyy-MM-dd HH:mm:ss");
        gsonBuilder.setPrettyPrinting();
        gson = gsonBuilder.create();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Add CORS headers
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        
        Map<String, Object> result = new HashMap<>();
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession(false);
        
        try {
            // Check if admin is authenticated
            if (session == null || session.getAttribute("adminId") == null) {
                result.put("authenticated", false);
                result.put("success", false);
                result.put("message", "Admin not authenticated");
                out.write(gson.toJson(result));
                return;
            }
            
            int adminId = (int) session.getAttribute("adminId");
            Admin admin = adminDAO.getAdminById(adminId);
            
            if (admin == null) {
                result.put("authenticated", false);
                result.put("success", false);
                result.put("message", "Admin not found");
                out.write(gson.toJson(result));
                return;
            }
            
            String pathInfo = request.getPathInfo();
            
            // Root path - Dashboard data
            if (pathInfo == null || pathInfo.equals("/")) {
                // Get dashboard statistics
                Map<String, Object> stats = adminDAO.getDashboardStats(admin);
                
                // Get recent applications
                List<Map<String, Object>> recentApps = adminDAO.getRecentApplications(admin, 10);
                
                // Get total counts for pagination
                int totalApplications = getTotalApplicationsCount();
                
                result.put("authenticated", true);
                result.put("success", true);
                result.put("stats", stats);
                result.put("recentApplications", recentApps);
                result.put("totalApplications", totalApplications);
                result.put("admin", getAdminInfo(admin));
                
            // Applications list with pagination
            } // In doGet method, look for this section:
            else if (pathInfo.equals("/applications")) {
                String status = request.getParameter("status");
                String search = request.getParameter("search");
                int page = parseIntParam(request.getParameter("page"), 1);
                int limit = parseIntParam(request.getParameter("limit"), 10);
                
                System.out.println("=== Applications Request ===");
                System.out.println("Status: " + status);
                System.out.println("Search: " + search);
                System.out.println("Page: " + page);
                System.out.println("Limit: " + limit);
                
                List<Map<String, Object>> applications = adminDAO.getAllApplications(admin, status, search, page, limit);
                int totalCount = getFilteredApplicationsCount(status, search);
                int totalPages = (int) Math.ceil((double) totalCount / limit);
                
                System.out.println("Applications found: " + applications.size());
                System.out.println("Total count: " + totalCount);
                
                result.put("success", true);
                result.put("applications", applications);
                result.put("page", page);
                result.put("limit", limit);
                result.put("totalCount", totalCount);
                result.put("totalPages", totalPages);
            } // Add this temporary debug endpoint by adding this case in your doGet method
         // Place it right after the /applications endpoint
            else if (pathInfo.equals("/debug/applications")) {
                System.out.println("=== DEBUG: Checking applications directly ===");
                
                // Direct SQL query to check what's in the database
                String debugSql = "SELECT a.id, a.reference_number, a.application_status, " +
                                  "u.first_name, u.last_name, u.email, " +
                                  "a.program_first_choice, a.college_first_choice, a.gwa " +
                                  "FROM applications a " +
                                  "JOIN users u ON a.user_id = u.id";
                
                List<Map<String, Object>> debugApps = new ArrayList<>();
                
                try (Connection conn = DatabaseConfig.getConnection();
                     PreparedStatement ps = conn.prepareStatement(debugSql);
                     ResultSet rs = ps.executeQuery()) {
                    
                    while (rs.next()) {
                        Map<String, Object> app = new HashMap<>();
                        app.put("id", rs.getInt("id"));
                        app.put("reference_number", rs.getString("reference_number"));
                        app.put("applicant", rs.getString("first_name") + " " + rs.getString("last_name"));
                        app.put("program_first_choice", rs.getString("program_first_choice"));
                        app.put("college_first_choice", rs.getString("college_first_choice"));
                        app.put("gwa", rs.getDouble("gwa"));
                        debugApps.add(app);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                
                result.put("success", true);
                result.put("debug_applications", debugApps);
                result.put("count", debugApps.size());
            }else if (pathInfo.startsWith("/application/")) {
                String appIdStr = pathInfo.substring("/application/".length());
                
                // Handle application with action
                if (appIdStr.contains("/")) {
                    String[] parts = appIdStr.split("/");
                    int appId = Integer.parseInt(parts[0]);
                    
                    Map<String, Object> applicationDetails = adminDAO.getApplicationDetails(admin, appId);
                    result.putAll(applicationDetails);
                } else {
                    int appId = Integer.parseInt(appIdStr);
                    Map<String, Object> applicationDetails = adminDAO.getApplicationDetails(admin, appId);
                    result.putAll(applicationDetails);
                }
                
            // Pending reviews for reviewers
            } else if (pathInfo.equals("/reviews/pending")) {
                int limit = parseIntParam(request.getParameter("limit"), 10);
                List<Map<String, Object>> pendingReviews = adminDAO.getPendingReviews(admin, limit);
                
                result.put("success", true);
                result.put("reviews", pendingReviews);
                
            // User management (admin only)
            } else if (pathInfo.equals("/users") && admin.isScholarshipAdministrator()) {
                String type = request.getParameter("type");
                String userStatus = request.getParameter("status");
                String search = request.getParameter("search");
                int page = parseIntParam(request.getParameter("page"), 1);
                int limit = parseIntParam(request.getParameter("limit"), 10);
                
                List<Map<String, Object>> users = adminDAO.getAllUsers(admin, type, userStatus, search, page, limit);
                int totalUsers = getTotalUsersCount(type, userStatus, search);
                int totalPages = (int) Math.ceil((double) totalUsers / limit);
                
                result.put("success", true);
                result.put("users", users);
                result.put("page", page);
                result.put("limit", limit);
                result.put("totalCount", totalUsers);
                result.put("totalPages", totalPages);
                
            // User stats
            } else if (pathInfo.equals("/users/stats") && admin.isScholarshipAdministrator()) {
                Map<String, Object> stats = getUserStats();
                result.put("success", true);
                result.putAll(stats);
                
            } else if (pathInfo.equals("/users") && !admin.isScholarshipAdministrator()) {
                result.put("success", false);
                result.put("message", "Access denied. Only Scholarship Administrators can manage users.");
            }
            
        } catch (NumberFormatException e) {
            result.put("success", false);
            result.put("message", "Invalid number format: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "An error occurred: " + e.getMessage());
        }
        
        out.write(gson.toJson(result));
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Add CORS headers
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        
        Map<String, Object> result = new HashMap<>();
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession(false);
        
        try {
            // Check if admin is authenticated
            if (session == null || session.getAttribute("adminId") == null) {
                result.put("success", false);
                result.put("message", "Admin not authenticated");
                out.write(gson.toJson(result));
                return;
            }
            
            int adminId = (int) session.getAttribute("adminId");
            Admin admin = adminDAO.getAdminById(adminId);
            
            if (admin == null) {
                result.put("success", false);
                result.put("message", "Admin not found");
                out.write(gson.toJson(result));
                return;
            }
            
            String pathInfo = request.getPathInfo();
            
            // Update application status
            if (pathInfo != null && pathInfo.startsWith("/application/")) {
                String[] pathParts = pathInfo.split("/");
                if (pathParts.length >= 4) {
                    String action = pathParts[3];
                    int appId = Integer.parseInt(pathParts[2]);
                    
                    switch (action) {
                        case "status":
                            handleStatusUpdate(request, response, admin, appId, adminId);
                            return;
                        case "document":
                            handleDocumentVerification(request, response, admin, appId, adminId);
                            return;
                        case "comment":
                            handleAddComment(request, response, admin, appId, adminId);
                            return;
                        default:
                            result.put("success", false);
                            result.put("message", "Invalid action");
                    }
                }
            } 
            // Create new user (admin only)
            else if (pathInfo != null && pathInfo.equals("/users") && admin.isScholarshipAdministrator()) {
                handleCreateUser(request, response, admin);
                return;
            }
            // Export applications
            else if (pathInfo != null && pathInfo.equals("/export") && admin.canExportData()) {
                handleExport(request, response, admin);
                return;
            } else {
                result.put("success", false);
                result.put("message", "Invalid endpoint or insufficient permissions");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "An error occurred: " + e.getMessage());
        }
        
        out.write(gson.toJson(result));
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Add CORS headers
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        
        Map<String, Object> result = new HashMap<>();
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession(false);
        
        try {
            // Check if admin is authenticated
            if (session == null || session.getAttribute("adminId") == null) {
                result.put("success", false);
                result.put("message", "Admin not authenticated");
                out.write(gson.toJson(result));
                return;
            }
            
            int adminId = (int) session.getAttribute("adminId");
            Admin admin = adminDAO.getAdminById(adminId);
            
            if (admin == null) {
                result.put("success", false);
                result.put("message", "Admin not found");
                out.write(gson.toJson(result));
                return;
            }
            
            String pathInfo = request.getPathInfo();
            
            // Update application
            if (pathInfo != null && pathInfo.startsWith("/application/")) {
                String[] pathParts = pathInfo.split("/");
                if (pathParts.length >= 4 && "update".equals(pathParts[3])) {
                    int appId = Integer.parseInt(pathParts[2]);
                    
                    // Read request body
                    StringBuilder sb = new StringBuilder();
                    BufferedReader reader = request.getReader();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    
                    JsonObject jsonData = JsonParser.parseString(sb.toString()).getAsJsonObject();
                    
                    // Update application in database
                    boolean updated = updateApplicationInDB(appId, jsonData, adminId);
                    
                    if (updated) {
                        result.put("success", true);
                        result.put("message", "Application updated successfully");
                    } else {
                        result.put("success", false);
                        result.put("message", "Failed to update application");
                    }
                } else {
                    result.put("success", false);
                    result.put("message", "Invalid update request");
                }
            }
            // Update user (admin only)
            else if (pathInfo != null && pathInfo.startsWith("/users/") && admin.isScholarshipAdministrator()) {
                int userId = Integer.parseInt(pathInfo.substring("/users/".length()));
                handleUpdateUser(request, response, userId, adminId);
                return;
            }
            // Update user status
            else if (pathInfo != null && pathInfo.contains("/status") && admin.isScholarshipAdministrator()) {
                String[] parts = pathInfo.split("/");
                if (parts.length >= 4) {
                    int userId = Integer.parseInt(parts[2]);
                    handleUpdateUserStatus(request, response, userId);
                    return;
                }
            } else {
                result.put("success", false);
                result.put("message", "Invalid endpoint or insufficient permissions");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "An error occurred: " + e.getMessage());
        }
        
        out.write(gson.toJson(result));
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Add CORS headers
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        
        Map<String, Object> result = new HashMap<>();
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession(false);
        
        try {
            // Check if admin is authenticated
            if (session == null || session.getAttribute("adminId") == null) {
                result.put("success", false);
                result.put("message", "Admin not authenticated");
                out.write(gson.toJson(result));
                return;
            }
            
            int adminId = (int) session.getAttribute("adminId");
            Admin admin = adminDAO.getAdminById(adminId);
            
            if (admin == null) {
                result.put("success", false);
                result.put("message", "Admin not found");
                out.write(gson.toJson(result));
                return;
            }
            
            String pathInfo = request.getPathInfo();
            
            // Delete application
            if (pathInfo != null && pathInfo.startsWith("/application/")) {
                String appIdStr = pathInfo.substring("/application/".length());
                
                try {
                    int appId = Integer.parseInt(appIdStr);
                    
                    // Check if admin has permission to delete
                    if (!admin.isScholarshipAdministrator()) {
                        result.put("success", false);
                        result.put("message", "Only Scholarship Administrators can delete applications");
                        out.write(gson.toJson(result));
                        return;
                    }
                    
                    boolean deleted = adminDAO.deleteApplication(admin, appId);
                    
                    if (deleted) {
                        result.put("success", true);
                        result.put("message", "Application deleted successfully");
                    } else {
                        result.put("success", false);
                        result.put("message", "Failed to delete application");
                    }
                    
                } catch (NumberFormatException e) {
                    result.put("success", false);
                    result.put("message", "Invalid application ID");
                }
            }
            // Delete user (admin only)
            else if (pathInfo != null && pathInfo.startsWith("/users/") && admin.isScholarshipAdministrator()) {
                int userId = Integer.parseInt(pathInfo.substring("/users/".length()));
                handleDeleteUser(request, response, userId);
                return;
            } else {
                result.put("success", false);
                result.put("message", "Invalid endpoint");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "An error occurred: " + e.getMessage());
        }
        
        out.write(gson.toJson(result));
    }
    
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setStatus(HttpServletResponse.SC_OK);
    }
    
    // Helper Methods
    
    private int parseIntParam(String param, int defaultValue) {
        if (param == null || param.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(param);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private int getTotalApplicationsCount() {
        String sql = "SELECT COUNT(*) as total FROM applications";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    private int getFilteredApplicationsCount(String status, String search) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) as total FROM applications a JOIN users u ON a.user_id = u.id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        
        if (status != null && !status.isEmpty() && !status.equals("all")) {
            sql.append(" AND a.application_status = ?");
            params.add(status);
        }
        
        if (search != null && !search.isEmpty()) {
            sql.append(" AND (u.first_name LIKE ? OR u.last_name LIKE ? OR u.email LIKE ? OR a.reference_number LIKE ?)");
            String searchPattern = "%" + search + "%";
            for (int i = 0; i < 4; i++) {
                params.add(searchPattern);
            }
        }
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    private int getTotalUsersCount(String type, String status, String search) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) as total FROM users WHERE 1=1");
        List<Object> params = new ArrayList<>();
        
        if (type != null && !type.isEmpty()) {
            sql.append(" AND user_type = ?");
            params.add(type);
        }
        
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        
        if (search != null && !search.isEmpty()) {
            sql.append(" AND (first_name LIKE ? OR last_name LIKE ? OR email LIKE ? OR user_id LIKE ?)");
            String searchPattern = "%" + search + "%";
            for (int i = 0; i < 4; i++) {
                params.add(searchPattern);
            }
        }
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    private Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT " +
                     "SUM(CASE WHEN user_type = 'admin' THEN 1 ELSE 0 END) as admins, " +
                     "SUM(CASE WHEN user_type = 'reviewer' THEN 1 ELSE 0 END) as reviewers, " +
                     "SUM(CASE WHEN user_type = 'applicant' THEN 1 ELSE 0 END) as applicants, " +
                     "SUM(CASE WHEN user_type = 'scholar' THEN 1 ELSE 0 END) as scholars, " +
                     "COUNT(*) as total " +
                     "FROM users";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                stats.put("admins", rs.getInt("admins") + rs.getInt("reviewers"));
                stats.put("applicants", rs.getInt("applicants"));
                stats.put("scholars", rs.getInt("scholars"));
                stats.put("total", rs.getInt("total"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
    
    private boolean updateApplicationInDB(int appId, JsonObject data, int adminId) {
        String sql = "UPDATE applications SET " +
                     "program_first = ?, " +
                     "college_first = ?, " +
                     "grade_12_gwa = ?, " +
                     "updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, getJsonString(data, "program"));
            ps.setString(2, getJsonString(data, "school"));
            ps.setDouble(3, getJsonDouble(data, "gwa", 0));
            ps.setInt(4, appId);
            
            int updated = ps.executeUpdate();
            
            if (updated > 0) {
                // Update user info if needed
                if (data.has("applicantName") || data.has("email") || data.has("phone")) {
                    updateUserFromApplication(conn, appId, data);
                }
                
                // Add timeline entry
                String status = getJsonString(data, "status");
                if (status != null && !status.isEmpty()) {
                    addTimelineEntry(conn, appId, adminId, "APPLICATION_UPDATED", 
                                    "Application details updated", null, status);
                }
                
                conn.commit();
                return true;
            }
            
            conn.rollback();
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    
    private void updateUserFromApplication(Connection conn, int appId, JsonObject data) throws SQLException {
        // Get user_id from application
        String getUserIdSql = "SELECT user_id FROM applications WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(getUserIdSql)) {
            ps.setInt(1, appId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("user_id");
                    
                    // Update user
                    StringBuilder updateSql = new StringBuilder("UPDATE users SET ");
                    List<Object> params = new ArrayList<>();
                    
                    if (data.has("applicantName")) {
                        String[] nameParts = data.get("applicantName").getAsString().split(" ", 2);
                        updateSql.append("first_name = ?, ");
                        params.add(nameParts[0]);
                        if (nameParts.length > 1) {
                            updateSql.append("last_name = ?, ");
                            params.add(nameParts[1]);
                        }
                    }
                    
                    if (data.has("email")) {
                        updateSql.append("email = ?, ");
                        params.add(data.get("email").getAsString());
                    }
                    
                    if (data.has("phone")) {
                        updateSql.append("phone = ?, ");
                        params.add(data.get("phone").getAsString());
                    }
                    
                    // Remove trailing comma and add WHERE clause
                    if (!params.isEmpty()) {
                        updateSql.setLength(updateSql.length() - 2);
                        updateSql.append(" WHERE id = ?");
                        params.add(userId);
                        
                        try (PreparedStatement updatePs = conn.prepareStatement(updateSql.toString())) {
                            for (int i = 0; i < params.size(); i++) {
                                updatePs.setObject(i + 1, params.get(i));
                            }
                            updatePs.executeUpdate();
                        }
                    }
                }
            }
        }
    }
    
    private void addTimelineEntry(Connection conn, int appId, int adminId, String action, 
                                  String description, String oldValue, String newValue) {
        String sql = "INSERT INTO application_timeline (application_id, user_id, action, description, old_value, new_value, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appId);
            ps.setInt(2, adminId);
            ps.setString(3, action);
            ps.setString(4, description);
            ps.setString(5, oldValue);
            ps.setString(6, newValue);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private String getJsonString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : null;
    }
    
    private double getJsonDouble(JsonObject json, String key, double defaultValue) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            try {
                return json.get(key).getAsDouble();
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    // Action Handlers
    
    private void handleStatusUpdate(HttpServletRequest request, HttpServletResponse response, 
                                   Admin admin, int appId, int adminId) throws IOException {
        
        Map<String, Object> result = new HashMap<>();
        
        if (!admin.canChangeApplicationStatus()) {
            result.put("success", false);
            result.put("message", "You don't have permission to change application status");
            response.getWriter().write(gson.toJson(result));
            return;
        }
        
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            JsonObject jsonData = JsonParser.parseString(sb.toString()).getAsJsonObject();
            String status = jsonData.get("status").getAsString();
            String notes = jsonData.has("notes") ? jsonData.get("notes").getAsString() : "";
            
            boolean updated = adminDAO.updateApplicationStatus(admin, appId, status, notes, adminId);
            
            if (updated) {
                result.put("success", true);
                result.put("message", "Application status updated successfully");
            } else {
                result.put("success", false);
                result.put("message", "Failed to update status");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        
        response.getWriter().write(gson.toJson(result));
    }
    
    private void handleDocumentVerification(HttpServletRequest request, HttpServletResponse response, 
                                          Admin admin, int appId, int adminId) throws IOException {
        
        Map<String, Object> result = new HashMap<>();
        
        if (!admin.canVerifyDocuments()) {
            result.put("success", false);
            result.put("message", "You don't have permission to verify documents");
            response.getWriter().write(gson.toJson(result));
            return;
        }
        
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            JsonObject jsonData = JsonParser.parseString(sb.toString()).getAsJsonObject();
            int docId = jsonData.get("documentId").getAsInt();
            String status = jsonData.get("status").getAsString();
            String rejectionReason = jsonData.has("rejectionReason") ? jsonData.get("rejectionReason").getAsString() : null;
            
            boolean updated = adminDAO.verifyDocument(admin, docId, appId, status, rejectionReason, adminId);
            
            if (updated) {
                result.put("success", true);
                result.put("message", "Document " + status + " successfully");
            } else {
                result.put("success", false);
                result.put("message", "Failed to update document");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        
        response.getWriter().write(gson.toJson(result));
    }
    
    private void handleAddComment(HttpServletRequest request, HttpServletResponse response, 
                                 Admin admin, int appId, int adminId) throws IOException {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            JsonObject jsonData = JsonParser.parseString(sb.toString()).getAsJsonObject();
            String comment = jsonData.get("comment").getAsString();
            
            // Add to timeline as comment
            try (Connection conn = DatabaseConfig.getConnection()) {
                addTimelineEntry(conn, appId, adminId, "COMMENT", comment, null, null);
            }
            
            result.put("success", true);
            result.put("message", "Comment added");
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        
        response.getWriter().write(gson.toJson(result));
    }
    
    private void handleCreateUser(HttpServletRequest request, HttpServletResponse response, Admin admin) throws IOException {
        Map<String, Object> result = new HashMap<>();
        
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            JsonObject jsonData = JsonParser.parseString(sb.toString()).getAsJsonObject();
            
            // Generate user_id
            String userId = generateUserId(jsonData.get("userType").getAsString());
            
            // Insert user
            String sql = "INSERT INTO users (user_id, first_name, last_name, email, phone, password_hash, user_type, status, created_at) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                
                ps.setString(1, userId);
                ps.setString(2, jsonData.get("firstName").getAsString());
                ps.setString(3, jsonData.get("lastName").getAsString());
                ps.setString(4, jsonData.get("email").getAsString());
                ps.setString(5, jsonData.has("phone") ? jsonData.get("phone").getAsString() : null);
                
                // Hash password (you should use proper hashing)
                String passwordHash = hashPassword(jsonData.get("password").getAsString());
                ps.setString(6, passwordHash);
                
                ps.setString(7, jsonData.get("userType").getAsString());
                ps.setString(8, jsonData.get("status").getAsString());
                
                int inserted = ps.executeUpdate();
                
                if (inserted > 0) {
                    result.put("success", true);
                    result.put("message", "User created successfully");
                    result.put("userId", userId);
                } else {
                    result.put("success", false);
                    result.put("message", "Failed to create user");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        
        response.getWriter().write(gson.toJson(result));
    }
    
    private void handleUpdateUser(HttpServletRequest request, HttpServletResponse response, 
                                 int userId, int adminId) throws IOException {
        Map<String, Object> result = new HashMap<>();
        
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            JsonObject jsonData = JsonParser.parseString(sb.toString()).getAsJsonObject();
            
            StringBuilder sql = new StringBuilder("UPDATE users SET ");
            List<Object> params = new ArrayList<>();
            
            if (jsonData.has("firstName")) {
                sql.append("first_name = ?, ");
                params.add(jsonData.get("firstName").getAsString());
            }
            if (jsonData.has("lastName")) {
                sql.append("last_name = ?, ");
                params.add(jsonData.get("lastName").getAsString());
            }
            if (jsonData.has("email")) {
                sql.append("email = ?, ");
                params.add(jsonData.get("email").getAsString());
            }
            if (jsonData.has("phone")) {
                sql.append("phone = ?, ");
                params.add(jsonData.get("phone").getAsString());
            }
            if (jsonData.has("userType")) {
                sql.append("user_type = ?, ");
                params.add(jsonData.get("userType").getAsString());
            }
            if (jsonData.has("status")) {
                sql.append("status = ?, ");
                params.add(jsonData.get("status").getAsString());
            }
            
            // Remove trailing comma
            sql.setLength(sql.length() - 2);
            sql.append(" WHERE id = ?");
            params.add(userId);
            
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                
                int updated = ps.executeUpdate();
                
                if (updated > 0) {
                    result.put("success", true);
                    result.put("message", "User updated successfully");
                } else {
                    result.put("success", false);
                    result.put("message", "Failed to update user");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        
        response.getWriter().write(gson.toJson(result));
    }
    
    private void handleUpdateUserStatus(HttpServletRequest request, HttpServletResponse response, int userId) throws IOException {
        Map<String, Object> result = new HashMap<>();
        
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            JsonObject jsonData = JsonParser.parseString(sb.toString()).getAsJsonObject();
            String status = jsonData.get("status").getAsString();
            
            String sql = "UPDATE users SET status = ? WHERE id = ?";
            
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                
                ps.setString(1, status);
                ps.setInt(2, userId);
                
                int updated = ps.executeUpdate();
                
                if (updated > 0) {
                    result.put("success", true);
                    result.put("message", "User status updated successfully");
                } else {
                    result.put("success", false);
                    result.put("message", "Failed to update user status");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        
        response.getWriter().write(gson.toJson(result));
    }
    
    private void handleDeleteUser(HttpServletRequest request, HttpServletResponse response, int userId) throws IOException {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Check if user has applications
            String checkSql = "SELECT COUNT(*) as count FROM applications WHERE user_id = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                
                checkPs.setInt(1, userId);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next() && rs.getInt("count") > 0) {
                        result.put("success", false);
                        result.put("message", "Cannot delete user with existing applications");
                        response.getWriter().write(gson.toJson(result));
                        return;
                    }
                }
            }
            
            // Delete user
            String sql = "DELETE FROM users WHERE id = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                
                ps.setInt(1, userId);
                
                int deleted = ps.executeUpdate();
                
                if (deleted > 0) {
                    result.put("success", true);
                    result.put("message", "User deleted successfully");
                } else {
                    result.put("success", false);
                    result.put("message", "Failed to delete user");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
        }
        
        response.getWriter().write(gson.toJson(result));
    }
    
    private void handleExport(HttpServletRequest request, HttpServletResponse response, Admin admin) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=applications.csv");
        
        String status = request.getParameter("status");
        String search = request.getParameter("search");
        
        List<Map<String, Object>> applications = adminDAO.getAllApplications(admin, status, search, 1, 10000);
        
        PrintWriter out = response.getWriter();
        
        // Write CSV header
        out.println("Reference Number,Applicant Name,Email,School,Program,GWA,Status,Created Date");
        
        // Write data
        for (Map<String, Object> app : applications) {
            out.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%.2f,\"%s\",\"%s\"%n",
                app.get("referenceNumber"),
                app.get("applicantName"),
                app.get("email"),
                app.get("school"),
                app.get("program"),
                app.get("gwa") != null ? app.get("gwa") : 0,
                app.get("status"),
                app.get("createdAt")
            );
        }
    }
    
    private Map<String, Object> getAdminInfo(Admin admin) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", admin.getId());
        info.put("fullName", admin.getFullName());
        info.put("email", admin.getEmail());
        info.put("role", admin.getRole());
        info.put("roleDisplayName", admin.getRoleDisplayName());
        info.put("isAdministrator", admin.isScholarshipAdministrator());
        info.put("isReviewer", admin.isReviewer());
        return info;
    }
    
    private String generateUserId(String userType) {
        String prefix;
        switch (userType.toLowerCase()) {
            case "admin": prefix = "ADM"; break;
            case "reviewer": prefix = "REV"; break;
            case "scholar": prefix = "SCH"; break;
            default: prefix = "APP";
        }
        
        String sql = "SELECT COUNT(*) as count FROM users WHERE user_id LIKE ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, prefix + "-%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count") + 1;
                    return String.format("%s-%04d", prefix, count);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return prefix + "-0001";
    }
    
    private String hashPassword(String password) {
        // In production, use proper password hashing like BCrypt
        // For demo purposes, we'll use a simple hash
        return password; // This should be replaced with actual hashing
    }
}