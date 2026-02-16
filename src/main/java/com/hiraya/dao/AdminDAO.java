package com.hiraya.dao;

import com.hiraya.config.DatabaseConfig;
import com.hiraya.model.Admin;
import com.hiraya.util.PasswordUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDAO {
    
    /**
     * Admin login authentication
     */
    public Admin login(String email, String password) {
        String sql = "SELECT u.*, a.id as admin_table_id, a.admin_id, a.role, a.department, a.permissions, a.last_login " +
                    "FROM users u " +
                    "LEFT JOIN admins a ON u.id = a.user_id " +
                    "WHERE u.email = ? AND u.user_type IN ('admin', 'reviewer') AND u.status = 'active'";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            System.out.println("✅ Database connected for admin login");
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            
            System.out.println("Executing query for email: " + email);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                System.out.println("Found admin user, verifying password...");
                
                if (PasswordUtil.verifyPassword(password, storedHash)) {
                    System.out.println("Password verified successfully");
                    Admin admin = mapResultSetToAdmin(rs);
                    
                    // Update last login
                    updateLastLogin(admin.getId());
                    
                    return admin;
                } else {
                    System.out.println("Password verification failed");
                }
            } else {
                System.out.println("No admin found with email: " + email);
            }
        } catch (SQLException e) {
            System.err.println("❌ SQL Error in admin login:");
            System.err.println("   Error Code: " + e.getErrorCode());
            System.err.println("   SQL State: " + e.getSQLState());
            System.err.println("   Message: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DatabaseConfig.closeConnection(conn, ps, rs);
        }
        return null;
    }
    
    /**
     * Get admin by ID
     */
    public Admin getAdminById(int id) {
        String sql = "SELECT u.*, a.id as admin_table_id, a.admin_id, a.role, a.department, a.permissions, a.last_login " +
                    "FROM users u " +
                    "LEFT JOIN admins a ON u.id = a.user_id " +
                    "WHERE u.id = ? AND u.user_type IN ('admin', 'reviewer')";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToAdmin(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.closeConnection(conn, ps, rs);
        }
        return null;
    }
    
    /**
     * Get admin by user ID
     */
    public Admin getAdminByUserId(int userId) {
        String sql = "SELECT u.*, a.id as admin_table_id, a.admin_id, a.role, a.department, a.permissions, a.last_login " +
                    "FROM users u " +
                    "LEFT JOIN admins a ON u.id = a.user_id " +
                    "WHERE u.id = ? AND u.user_type IN ('admin', 'reviewer')";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToAdmin(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.closeConnection(conn, ps, rs);
        }
        return null;
    }
    
    /**
     * Update admin's last login time
     */
    private void updateLastLogin(int userId) {
        String sql = "UPDATE admins SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";
        
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.executeUpdate();
            System.out.println("Updated last login for admin user: " + userId);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.closeConnection(conn, ps);
        }
    }
    
    /**
     * Get dashboard statistics
     */
    public Map<String, Object> getDashboardStats(Admin admin) {
        Map<String, Object> stats = new HashMap<>();
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            
            String sql = "SELECT " +
                        "COUNT(*) as total_applications, " +
                        "SUM(CASE WHEN application_status = 'submitted' THEN 1 ELSE 0 END) as pending_review, " +
                        "SUM(CASE WHEN application_status = 'under_review' THEN 1 ELSE 0 END) as under_review, " +
                        "SUM(CASE WHEN application_status = 'interview' THEN 1 ELSE 0 END) as interview, " +
                        "SUM(CASE WHEN application_status = 'approved' THEN 1 ELSE 0 END) as approved, " +
                        "SUM(CASE WHEN application_status = 'declined' THEN 1 ELSE 0 END) as declined, " +
                        "SUM(CASE WHEN application_status = 'draft' THEN 1 ELSE 0 END) as draft_applications " +
                        "FROM applications";
            
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                stats.put("totalApplications", rs.getInt("total_applications"));
                stats.put("pendingReview", rs.getInt("pending_review"));
                stats.put("underReview", rs.getInt("under_review"));
                stats.put("interview", rs.getInt("interview"));
                stats.put("approved", rs.getInt("approved"));
                stats.put("declined", rs.getInt("declined"));
                stats.put("draftApplications", rs.getInt("draft_applications"));
                
                // For admin dashboard
                if (admin.isScholarshipAdministrator()) {
                    stats.put("submittedApplications", rs.getInt("pending_review"));
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.closeConnection(conn, ps, rs);
        }
        
        return stats;
    }
    
    /**
     * Get total applications count
     */
    public int getTotalApplicationsCount() {
        String sql = "SELECT COUNT(*) as total FROM applications";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.closeConnection(conn, ps, rs);
        }
        return 0;
    }
    
    /**
     * Get filtered applications count
     */
    public int getFilteredApplicationsCount(String status, String search) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) as total FROM applications WHERE 1=1");
        List<Object> params = new ArrayList<>();
        
        if (status != null && !status.isEmpty() && !status.equals("all")) {
            sql.append(" AND application_status = ?");
            params.add(status);
        }
        
        if (search != null && !search.isEmpty()) {
            sql.append(" AND (first_name LIKE ? OR last_name LIKE ? OR email LIKE ? OR reference_number LIKE ?)");
            String searchPattern = "%" + search + "%";
            for (int i = 0; i < 4; i++) {
                params.add(searchPattern);
            }
        }
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            ps = conn.prepareStatement(sql.toString());
            
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.closeConnection(conn, ps, rs);
        }
        return 0;
    }
    
    /**
     * Get recent applications for dashboard
     */
    public List<Map<String, Object>> getRecentApplications(Admin admin, int limit) {
        List<Map<String, Object>> applications = new ArrayList<>();
        String sql = "SELECT id, reference_number, application_status, created_at, " +
                     "first_name, middle_name, last_name, email, " +
                     "college_first, program_first, grade_12_gwa " +
                     "FROM applications " +
                     "WHERE application_status IN ('submitted', 'under_review', 'approved', 'declined') " +
                     "ORDER BY created_at DESC LIMIT ?";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, limit);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> app = new HashMap<>();
                app.put("id", rs.getInt("id"));
                app.put("referenceNumber", rs.getString("reference_number"));
                app.put("reference_number", rs.getString("reference_number"));
                
                String fullName = buildFullName(
                    rs.getString("first_name"),
                    rs.getString("middle_name"),
                    rs.getString("last_name")
                );
                app.put("applicantName", fullName);
                
                app.put("email", rs.getString("email"));
                app.put("status", rs.getString("application_status"));
                app.put("application_status", rs.getString("application_status"));
                app.put("createdAt", rs.getTimestamp("created_at"));
                app.put("created_at", rs.getTimestamp("created_at"));
                app.put("school", rs.getString("college_first"));
                app.put("program", rs.getString("program_first"));
                app.put("gwa", rs.getBigDecimal("grade_12_gwa"));
                app.put("grade_12_gwa", rs.getBigDecimal("grade_12_gwa"));
                
                applications.add(app);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.closeConnection(conn, ps, rs);
        }
        return applications;
    }
    
    /**
     * Get all applications with filters
     */
    public List<Map<String, Object>> getAllApplications(Admin admin, String status, String search, int page, int limit) {
        List<Map<String, Object>> applications = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        
        System.out.println("===== getAllApplications called =====");
        System.out.println("Status filter: " + status);
        System.out.println("Search filter: " + search);
        System.out.println("Page: " + page + ", Limit: " + limit);
        
        // Select all columns from applications table
        sql.append("SELECT * FROM applications ");
        sql.append("WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();
        
        // Apply status filter
        if (status != null && !status.isEmpty() && !status.equals("all")) {
            sql.append("AND application_status = ? ");
            params.add(status);
        }
        
        // Apply search filter
        if (search != null && !search.isEmpty()) {
            sql.append("AND (first_name LIKE ? OR last_name LIKE ? OR email LIKE ? OR reference_number LIKE ?) ");
            String searchPattern = "%" + search + "%";
            for (int i = 0; i < 4; i++) {
                params.add(searchPattern);
            }
        }
        
        // Add pagination
        sql.append("ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add((page - 1) * limit);
        
        System.out.println("SQL Query: " + sql.toString());
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            ps = conn.prepareStatement(sql.toString());
            
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            
            rs = ps.executeQuery();
            
            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                
                Map<String, Object> app = new HashMap<>();
                
                // Basic Information
                app.put("id", rs.getInt("id"));
                app.put("application_id", rs.getString("application_id"));
                app.put("user_id", rs.getInt("user_id"));
                app.put("referenceNumber", rs.getString("reference_number"));
                app.put("reference_number", rs.getString("reference_number"));
                app.put("application_status", rs.getString("application_status"));
                app.put("status", rs.getString("application_status"));
                app.put("createdAt", rs.getTimestamp("created_at"));
                app.put("created_at", rs.getTimestamp("created_at"));
                app.put("updated_at", rs.getTimestamp("updated_at"));
                app.put("submission_date", rs.getTimestamp("submission_date"));
                app.put("last_saved", rs.getTimestamp("last_saved"));
                app.put("admin_comment", rs.getString("admin_comment"));
                
                // Personal Information
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                String middleName = rs.getString("middle_name");
                
                String fullName = buildFullName(firstName, middleName, lastName);
                
                app.put("applicantName", fullName);
                app.put("first_name", firstName);
                app.put("middle_name", middleName);
                app.put("last_name", lastName);
                app.put("sex", rs.getString("sex"));
                app.put("birthdate", rs.getDate("birthdate"));
                app.put("age", rs.getInt("age"));
                app.put("place_of_birth", rs.getString("place_of_birth"));
                app.put("height", rs.getBigDecimal("height"));
                app.put("weight", rs.getBigDecimal("weight"));
                app.put("mobile_number", rs.getString("mobile_number"));
                app.put("email", rs.getString("email"));
                app.put("facebook_url", rs.getString("facebook_url"));
                
                // Present Address
                app.put("present_region", rs.getString("present_region"));
                app.put("present_province", rs.getString("present_province"));
                app.put("present_municipality", rs.getString("present_municipality"));
                app.put("present_barangay", rs.getString("present_barangay"));
                app.put("present_house_number", rs.getString("present_house_number"));
                app.put("present_street", rs.getString("present_street"));
                app.put("present_zip_code", rs.getString("present_zip_code"));
                
                // Permanent Address
                app.put("permanent_region", rs.getString("permanent_region"));
                app.put("permanent_province", rs.getString("permanent_province"));
                app.put("permanent_municipality", rs.getString("permanent_municipality"));
                app.put("permanent_barangay", rs.getString("permanent_barangay"));
                app.put("permanent_house_number", rs.getString("permanent_house_number"));
                app.put("permanent_street", rs.getString("permanent_street"));
                app.put("permanent_zip_code", rs.getString("permanent_zip_code"));
                
                // Educational Background - JHS
                app.put("jhs_name", rs.getString("jhs_name"));
                app.put("jhs_school_id", rs.getString("jhs_school_id"));
                app.put("jhs_type", rs.getString("jhs_type"));
                
                // Educational Background - SHS
                app.put("shs_name", rs.getString("shs_name"));
                app.put("shs_school_id", rs.getString("shs_school_id"));
                app.put("shs_type", rs.getString("shs_type"));
                app.put("track", rs.getString("track"));
                app.put("strand", rs.getString("strand"));
                
                // Academic Performance
                app.put("grade_12_gwa", rs.getBigDecimal("grade_12_gwa"));
                app.put("gwa", rs.getBigDecimal("grade_12_gwa"));
                app.put("honors_received", rs.getString("honors_received"));
                
                // College Choices
                app.put("college_first", rs.getString("college_first"));
                app.put("college_second", rs.getString("college_second"));
                app.put("college_third", rs.getString("college_third"));
                app.put("school", rs.getString("college_first"));
                
                // Program Choices
                app.put("program_first", rs.getString("program_first"));
                app.put("program_second", rs.getString("program_second"));
                app.put("program_third", rs.getString("program_third"));
                app.put("program", rs.getString("program_first"));
                
                // Essay
                app.put("essay", rs.getString("essay"));
                
                applications.add(app);
            }
            
            System.out.println("Total rows found: " + rowCount);
            System.out.println("Returning " + applications.size() + " applications");
            
        } catch (SQLException e) {
            System.err.println("SQL Error in getAllApplications:");
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DatabaseConfig.closeConnection(conn, ps, rs);
        }
        
        return applications;
    }
    
    /**
     * Get application details by ID
     */
    public Map<String, Object> getApplicationDetails(Admin admin, int appId) {
        Map<String, Object> details = new HashMap<>();
        String sql = "SELECT * FROM applications WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, appId);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                Map<String, Object> app = new HashMap<>();
                ResultSetMetaData meta = rs.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    String columnName = meta.getColumnLabel(i);
                    app.put(columnName, rs.getObject(i));
                }
                
                // Format full name
                String fullName = buildFullName(
                    rs.getString("first_name"),
                    rs.getString("middle_name"),
                    rs.getString("last_name")
                );
                app.put("applicantName", fullName);
                
                details.put("application", app);
                details.put("success", true);
                
                // Add permissions
                Map<String, Boolean> permissions = new HashMap<>();
                permissions.put("canChangeStatus", admin.canChangeApplicationStatus());
                permissions.put("canVerifyDocuments", admin.canVerifyDocuments());
                permissions.put("canExportData", admin.canExportData());
                permissions.put("canManageUsers", admin.canManageUsers());
                permissions.put("canReviewApplications", admin.canReviewApplications());
                details.put("permissions", permissions);
                
            } else {
                details.put("success", false);
                details.put("message", "Application not found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            details.put("success", false);
            details.put("message", "Database error: " + e.getMessage());
        } finally {
            DatabaseConfig.closeConnection(conn, ps, rs);
        }
        return details;
    }
    
    /**
     * Delete an application (only Scholarship Administrator)
     */
    public boolean deleteApplication(Admin admin, int appId) {
        if (!admin.canManageUsers()) {
            return false;
        }
        
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            
            // Delete the application
            String deleteAppSql = "DELETE FROM applications WHERE id = ?";
            ps = conn.prepareStatement(deleteAppSql);
            ps.setInt(1, appId);
            
            int deleted = ps.executeUpdate();
            
            return deleted > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConfig.closeConnection(conn, ps);
        }
    }
    
    /**
     * Get applications pending review
     */
    public List<Map<String, Object>> getPendingReviews(Admin admin, int limit) {
        List<Map<String, Object>> applications = new ArrayList<>();
        String sql = "SELECT id, reference_number, application_status, " +
                     "first_name, middle_name, last_name, email, " +
                     "created_at, submission_date " +
                     "FROM applications " +
                     "WHERE application_status IN ('submitted', 'under_review') " +
                     "ORDER BY submission_date ASC LIMIT ?";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, limit);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> app = new HashMap<>();
                app.put("id", rs.getInt("id"));
                app.put("referenceNumber", rs.getString("reference_number"));
                
                String fullName = buildFullName(
                    rs.getString("first_name"),
                    rs.getString("middle_name"),
                    rs.getString("last_name")
                );
                app.put("applicantName", fullName);
                
                app.put("email", rs.getString("email"));
                app.put("status", rs.getString("application_status"));
                app.put("submittedAt", rs.getTimestamp("submission_date"));
                
                applications.add(app);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.closeConnection(conn, ps, rs);
        }
        return applications;
    }
    
    /**
     * Update application status
     */
    public boolean updateApplicationStatus(Admin admin, int appId, String status, String comment, int adminId) {
        if (!admin.canChangeApplicationStatus()) {
            return false;
        }
        
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            
            // Update status and admin_comment
            String updateSql = "UPDATE applications SET application_status = ?, admin_comment = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            ps = conn.prepareStatement(updateSql);
            ps.setString(1, status);
            ps.setString(2, comment);
            ps.setInt(3, appId);
            
            int updated = ps.executeUpdate();
            
            return updated > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConfig.closeConnection(conn, ps);
        }
    }
    
    /**
     * Verify document
     * Note: This assumes you have a documents table. If documents are stored in the applications table,
     * you'll need to modify this method accordingly.
     */
    public boolean verifyDocument(Admin admin, int docId, int appId, String status, String rejectionReason, int adminId) {
        if (!admin.canVerifyDocuments()) {
            return false;
        }
        
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            
            // Check if documents table exists, if not, update the application table directly
            String sql = "UPDATE documents SET verification_status = ?, verified_by = ?, verified_at = CURRENT_TIMESTAMP, " +
                        "rejection_reason = ? WHERE id = ? AND application_id = ?";
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setInt(2, adminId);
            ps.setString(3, rejectionReason);
            ps.setInt(4, docId);
            ps.setInt(5, appId);
            
            int updated = ps.executeUpdate();
            
            return updated > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConfig.closeConnection(conn, ps);
        }
    }
    
    /**
     * Get document path
     */
    public String getDocumentPath(int appId, String documentKey) {
        // Document keys in the applications table are stored as column names
        String sql = "SELECT " + documentKey + " FROM applications WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, appId);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getString(documentKey);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.closeConnection(conn, ps, rs);
        }
        
        return null;
    }
    
    /**
     * Get all users (only Scholarship Administrator)
     */
    public List<Map<String, Object>> getAllUsers(Admin admin, String type, String status, String search, int page, int limit) {
        if (!admin.canManageUsers()) {
            return new ArrayList<>();
        }
        
        List<Map<String, Object>> users = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT u.*, " +
            "(SELECT COUNT(*) FROM applications WHERE user_id = u.id) as application_count " +
            "FROM users u " +
            "WHERE 1=1 "
        );
        
        List<Object> params = new ArrayList<>();
        
        if (type != null && !type.isEmpty() && !type.equals("all")) {
            sql.append("AND u.user_type = ? ");
            params.add(type);
        }
        
        if (status != null && !status.isEmpty() && !status.equals("all")) {
            sql.append("AND u.status = ? ");
            params.add(status);
        }
        
        if (search != null && !search.isEmpty()) {
            sql.append("AND (u.first_name LIKE ? OR u.last_name LIKE ? OR u.email LIKE ? OR u.user_id LIKE ?) ");
            String searchPattern = "%" + search + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        sql.append("ORDER BY u.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add((page - 1) * limit);
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            ps = conn.prepareStatement(sql.toString());
            
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            
            rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", rs.getInt("id"));
                user.put("userId", rs.getString("user_id"));
                user.put("firstName", rs.getString("first_name"));
                user.put("lastName", rs.getString("last_name"));
                user.put("fullName", rs.getString("first_name") + " " + rs.getString("last_name"));
                user.put("email", rs.getString("email"));
                user.put("phone", rs.getString("phone"));
                user.put("userType", rs.getString("user_type"));
                user.put("status", rs.getString("status"));
                user.put("createdAt", rs.getTimestamp("created_at"));
                user.put("applicationCount", rs.getInt("application_count"));
                users.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.closeConnection(conn, ps, rs);
        }
        
        return users;
    }
    
    /**
     * Get total users count
     */
    public int getTotalUsersCount(String type, String status, String search) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) as total FROM users WHERE 1=1");
        List<Object> params = new ArrayList<>();
        
        if (type != null && !type.isEmpty() && !type.equals("all")) {
            sql.append(" AND user_type = ?");
            params.add(type);
        }
        
        if (status != null && !status.isEmpty() && !status.equals("all")) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        
        if (search != null && !search.isEmpty()) {
            sql.append(" AND (first_name LIKE ? OR last_name LIKE ? OR email LIKE ? OR user_id LIKE ?)");
            String searchPattern = "%" + search + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            ps = conn.prepareStatement(sql.toString());
            
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.closeConnection(conn, ps, rs);
        }
        return 0;
    }
    
    /**
     * Get user statistics
     */
    public Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT " +
                     "SUM(CASE WHEN user_type IN ('admin', 'reviewer') THEN 1 ELSE 0 END) as admins, " +
                     "SUM(CASE WHEN user_type = 'applicant' THEN 1 ELSE 0 END) as applicants, " +
                     "SUM(CASE WHEN user_type = 'scholar' THEN 1 ELSE 0 END) as scholars, " +
                     "COUNT(*) as total " +
                     "FROM users";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                stats.put("admins", rs.getInt("admins"));
                stats.put("applicants", rs.getInt("applicants"));
                stats.put("scholars", rs.getInt("scholars"));
                stats.put("total", rs.getInt("total"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.closeConnection(conn, ps, rs);
        }
        return stats;
    }
    
    /**
     * Helper method to build full name
     */
    private String buildFullName(String firstName, String middleName, String lastName) {
        StringBuilder fullName = new StringBuilder();
        
        if (firstName != null && !firstName.isEmpty()) {
            fullName.append(firstName);
        }
        
        if (middleName != null && !middleName.isEmpty()) {
            fullName.append(" ").append(middleName);
        }
        
        if (lastName != null && !lastName.isEmpty()) {
            fullName.append(" ").append(lastName);
        }
        
        return fullName.toString().trim();
    }
    
    /**
     * Map ResultSet to Admin object
     */
    private Admin mapResultSetToAdmin(ResultSet rs) throws SQLException {
        Admin admin = new Admin();
        admin.setId(rs.getInt("u.id"));
        admin.setUserId(rs.getString("u.user_id"));
        admin.setFirstName(rs.getString("u.first_name"));
        admin.setLastName(rs.getString("u.last_name"));
        admin.setEmail(rs.getString("u.email"));
        admin.setPhone(rs.getString("u.phone"));
        admin.setPasswordHash(rs.getString("u.password_hash"));
        admin.setUserType(rs.getString("u.user_type"));
        admin.setStatus(rs.getString("u.status"));
        admin.setCreatedAt(rs.getTimestamp("u.created_at"));
        admin.setUpdatedAt(rs.getTimestamp("u.updated_at"));
        
        // Admin specific fields
        admin.setAdminId(rs.getString("admin_id"));
        admin.setRole(rs.getString("role"));
        admin.setDepartment(rs.getString("department"));
        admin.setPermissions(rs.getString("permissions"));
        admin.setLastLogin(rs.getTimestamp("last_login"));
        
        return admin;
    }
    
    /**
     * Create a new admin user
     */
    public boolean createAdmin(Admin admin, String password) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);
            
            // First insert into users table
            String userSql = "INSERT INTO users (user_id, first_name, last_name, email, phone, password_hash, user_type, status, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
            
            ps = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, admin.getUserId());
            ps.setString(2, admin.getFirstName());
            ps.setString(3, admin.getLastName());
            ps.setString(4, admin.getEmail());
            ps.setString(5, admin.getPhone());
            ps.setString(6, PasswordUtil.hashPassword(password));
            ps.setString(7, admin.getUserType());
            
            int userInserted = ps.executeUpdate();
            
            if (userInserted == 0) {
                conn.rollback();
                return false;
            }
            
            // Get the generated user ID
            rs = ps.getGeneratedKeys();
            int userId = -1;
            if (rs.next()) {
                userId = rs.getInt(1);
            }
            
            if (userId == -1) {
                conn.rollback();
                return false;
            }
            
            // Then insert into admins table
            String adminSql = "INSERT INTO admins (user_id, admin_id, role, department, permissions, created_at, updated_at) " +
                             "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
            
            ps = conn.prepareStatement(adminSql);
            ps.setInt(1, userId);
            ps.setString(2, admin.getAdminId());
            ps.setString(3, admin.getRole());
            ps.setString(4, admin.getDepartment());
            ps.setString(5, admin.getPermissions());
            
            int adminInserted = ps.executeUpdate();
            
            if (adminInserted > 0) {
                conn.commit();
                return true;
            } else {
                conn.rollback();
                return false;
            }
            
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
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
    }
    
    /**
     * Update admin information
     */
    public boolean updateAdmin(Admin admin) {
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);
            
            // Update users table
            String userSql = "UPDATE users SET first_name = ?, last_name = ?, email = ?, phone = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            ps = conn.prepareStatement(userSql);
            ps.setString(1, admin.getFirstName());
            ps.setString(2, admin.getLastName());
            ps.setString(3, admin.getEmail());
            ps.setString(4, admin.getPhone());
            ps.setInt(5, admin.getId());
            
            int userUpdated = ps.executeUpdate();
            
            if (userUpdated == 0) {
                conn.rollback();
                return false;
            }
            
            // Update admins table
            String adminSql = "UPDATE admins SET role = ?, department = ?, permissions = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
            ps = conn.prepareStatement(adminSql);
            ps.setString(1, admin.getRole());
            ps.setString(2, admin.getDepartment());
            ps.setString(3, admin.getPermissions());
            ps.setInt(4, admin.getId());
            
            int adminUpdated = ps.executeUpdate();
            
            if (adminUpdated > 0) {
                conn.commit();
                return true;
            } else {
                conn.rollback();
                return false;
            }
            
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            return false;
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
    }
    
    /**
     * Update admin password
     */
    public boolean updatePassword(int adminId, String newPassword) {
        String sql = "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, PasswordUtil.hashPassword(newPassword));
            ps.setInt(2, adminId);
            
            int updated = ps.executeUpdate();
            return updated > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConfig.closeConnection(conn, ps);
        }
    }
}