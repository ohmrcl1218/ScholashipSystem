package com.hiraya.dao;

import com.hiraya.config.DatabaseConfig;
import com.hiraya.model.Admin;
import com.hiraya.util.PasswordUtil;
import com.hiraya.dao.DocumentDAO;
import com.hiraya.dao.ApplicationDAO;
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
                    "WHERE u.email = ? AND u.user_type = 'admin' AND u.status = 'active'";
        
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
                    "WHERE u.id = ? AND u.user_type = 'admin'";
        
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
     * Get dashboard statistics with role-based access
     */
    public Map<String, Object> getDashboardStats(Admin admin) {
        Map<String, Object> stats = new HashMap<>();
        String sql;
        
        if (admin.isScholarshipAdministrator()) {
            // Admin sees all stats
            sql = "SELECT " +
                  "(SELECT COUNT(*) FROM users WHERE user_type = 'applicant') as total_applicants, " +
                  "(SELECT COUNT(*) FROM users WHERE user_type = 'applicant' AND DATE(created_at) = CURDATE()) as new_applicants_today, " +
                  "(SELECT COUNT(*) FROM applications) as total_applications, " +
                  "(SELECT COUNT(*) FROM applications WHERE application_status = 'draft') as draft_applications, " +
                  "(SELECT COUNT(*) FROM applications WHERE application_status = 'submitted') as submitted_applications, " +
                  "(SELECT COUNT(*) FROM applications WHERE application_status = 'under_review') as under_review, " +
                  "(SELECT COUNT(*) FROM applications WHERE application_status = 'interview') as for_interview, " +
                  "(SELECT COUNT(*) FROM applications WHERE application_status = 'approved') as approved, " +
                  "(SELECT COUNT(*) FROM applications WHERE application_status = 'declined') as declined, " +
                  "(SELECT COUNT(*) FROM documents WHERE upload_status = 'pending') as pending_documents, " +
                  "(SELECT COUNT(*) FROM applications WHERE DATE(created_at) = CURDATE()) as applications_today, " +
                  "(SELECT COUNT(*) FROM applications WHERE DATE(submission_date) = CURDATE()) as submissions_today";
        } else {
            // Reviewer sees limited stats
            sql = "SELECT " +
                  "(SELECT COUNT(*) FROM applications WHERE application_status = 'submitted') as pending_review, " +
                  "(SELECT COUNT(*) FROM applications WHERE application_status = 'under_review') as under_review, " +
                  "(SELECT COUNT(*) FROM applications WHERE application_status = 'interview') as for_interview, " +
                  "(SELECT COUNT(*) FROM applications WHERE application_status = 'approved') as approved, " +
                  "(SELECT COUNT(*) FROM applications WHERE application_status = 'declined') as declined, " +
                  "(SELECT COUNT(*) FROM documents WHERE upload_status = 'pending') as pending_documents";
        }
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                if (admin.isScholarshipAdministrator()) {
                    stats.put("totalApplicants", rs.getInt("total_applicants"));
                    stats.put("newApplicantsToday", rs.getInt("new_applicants_today"));
                    stats.put("totalApplications", rs.getInt("total_applications"));
                    stats.put("draftApplications", rs.getInt("draft_applications"));
                    stats.put("submittedApplications", rs.getInt("submitted_applications"));
                    stats.put("underReview", rs.getInt("under_review"));
                    stats.put("forInterview", rs.getInt("for_interview"));
                    stats.put("approved", rs.getInt("approved"));
                    stats.put("declined", rs.getInt("declined"));
                    stats.put("pendingDocuments", rs.getInt("pending_documents"));
                    stats.put("applicationsToday", rs.getInt("applications_today"));
                    stats.put("submissionsToday", rs.getInt("submissions_today"));
                } else {
                    stats.put("pendingReview", rs.getInt("pending_review"));
                    stats.put("underReview", rs.getInt("under_review"));
                    stats.put("forInterview", rs.getInt("for_interview"));
                    stats.put("approved", rs.getInt("approved"));
                    stats.put("declined", rs.getInt("declined"));
                    stats.put("pendingDocuments", rs.getInt("pending_documents"));
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
     * Get recent applications with role-based access - CORRECT VERSION with your actual column names
     */
    public List<Map<String, Object>> getRecentApplications(Admin admin, int limit) {
        List<Map<String, Object>> applications = new ArrayList<>();
        String sql;
        
        if (admin.isScholarshipAdministrator()) {
            sql = "SELECT a.*, u.first_name, u.last_name, u.email, u.phone, " +
                  "u.user_id as applicant_user_id " +
                  "FROM applications a " +
                  "JOIN users u ON a.user_id = u.id " +
                  "ORDER BY a.created_at DESC LIMIT ?";
        } else {
            sql = "SELECT a.id, a.reference_number, a.application_status, a.created_at, a.submission_date, " +
                  "u.first_name, u.last_name, u.email, " +
                  "a.program_first as program, a.college_first as school, a.grade_12_gwa as gwa " +
                  "FROM applications a " +
                  "JOIN users u ON a.user_id = u.id " +
                  "ORDER BY a.created_at DESC LIMIT ?";
        }
        
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
                app.put("applicantName", rs.getString("first_name") + " " + rs.getString("last_name"));
                app.put("email", rs.getString("email"));
                app.put("status", rs.getString("application_status"));
                app.put("createdAt", rs.getTimestamp("created_at"));
                
                if (admin.isScholarshipAdministrator()) {
                    // For administrators, get all fields directly from the applications table
                    app.put("program", rs.getString("program_first"));
                    app.put("school", rs.getString("college_first"));
                    app.put("gwa", rs.getDouble("grade_12_gwa"));
                    app.put("phone", rs.getString("phone"));
                } else {
                    // For reviewers, use the aliased columns
                    app.put("program", rs.getString("program"));
                    app.put("school", rs.getString("school"));
                    app.put("gwa", rs.getDouble("gwa"));
                }
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
     * Get all applications with filters (role-based) - CORRECT VERSION with your actual column names
     */
    public List<Map<String, Object>> getAllApplications(Admin admin, String status, String search, int page, int limit) {
        List<Map<String, Object>> applications = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        
        System.out.println("===== getAllApplications called =====");
        System.out.println("Admin role: " + (admin != null ? admin.getRole() : "null"));
        System.out.println("Status filter: " + status);
        System.out.println("Search filter: " + search);
        System.out.println("Page: " + page + ", Limit: " + limit);
        
        if (admin.isScholarshipAdministrator()) {
            sql.append("SELECT a.*, u.first_name, u.last_name, u.email, u.phone, u.user_id as applicant_user_id ");
        } else {
            sql.append("SELECT a.id, a.reference_number, a.application_status, a.created_at, a.submission_date, ");
            sql.append("u.first_name, u.last_name, u.email, ");
            sql.append("a.program_first as program, a.college_first as school, a.grade_12_gwa as gwa ");
        }
        sql.append("FROM applications a ");
        sql.append("JOIN users u ON a.user_id = u.id ");
        sql.append("WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();
        
        if (status != null && !status.isEmpty() && !status.equals("all")) {
            sql.append("AND a.application_status = ? ");
            params.add(status);
        }
        
        if (search != null && !search.isEmpty()) {
            sql.append("AND (u.first_name LIKE ? OR u.last_name LIKE ? OR u.email LIKE ? OR a.reference_number LIKE ?) ");
            String searchPattern = "%" + search + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        sql.append("ORDER BY a.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add((page - 1) * limit);
        
        System.out.println("SQL Query: " + sql.toString());
        System.out.println("Parameters: " + params);
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            ps = conn.prepareStatement(sql.toString());
            
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
                System.out.println("Param " + (i+1) + ": " + params.get(i));
            }
            
            rs = ps.executeQuery();
            
            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                
                Map<String, Object> app = new HashMap<>();
                
                // Common fields for all admin types
                app.put("id", rs.getInt("id"));
                app.put("referenceNumber", rs.getString("reference_number"));
                app.put("applicantName", rs.getString("first_name") + " " + rs.getString("last_name"));
                app.put("email", rs.getString("email"));
                app.put("status", rs.getString("application_status"));
                app.put("createdAt", rs.getTimestamp("created_at"));
                
                if (admin.isScholarshipAdministrator()) {
                    // For administrators, get all fields directly from the applications table
                    app.put("program", rs.getString("program_first"));
                    app.put("school", rs.getString("college_first"));
                    app.put("gwa", rs.getDouble("grade_12_gwa"));
                    app.put("phone", rs.getString("phone"));
                    
                    System.out.println("  Row " + rowCount + ": ID=" + rs.getInt("id") + 
                                     ", Name=" + rs.getString("first_name") + " " + rs.getString("last_name") + 
                                     ", Program=" + rs.getString("program_first") + 
                                     ", School=" + rs.getString("college_first") +
                                     ", GWA=" + rs.getDouble("grade_12_gwa") +
                                     ", Status=" + rs.getString("application_status"));
                } else {
                    // For reviewers, use the aliased columns
                    app.put("program", rs.getString("program"));
                    app.put("school", rs.getString("school"));
                    app.put("gwa", rs.getDouble("gwa"));
                    
                    System.out.println("  Row " + rowCount + ": ID=" + rs.getInt("id") + 
                                     ", Name=" + rs.getString("first_name") + " " + rs.getString("last_name") + 
                                     ", Program=" + rs.getString("program") + 
                                     ", School=" + rs.getString("school") +
                                     ", GWA=" + rs.getDouble("gwa") +
                                     ", Status=" + rs.getString("application_status"));
                }
                
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
     * Get application details by ID (role-based)
     */
    public Map<String, Object> getApplicationDetails(Admin admin, int appId) {
        Map<String, Object> details = new HashMap<>();
        String sql;
        
        if (admin.isScholarshipAdministrator()) {
            sql = "SELECT a.*, u.* FROM applications a " +
                  "JOIN users u ON a.user_id = u.id WHERE a.id = ?";
        } else {
            sql = "SELECT a.*, u.first_name, u.last_name, u.email, u.user_id " +
                  "FROM applications a " +
                  "JOIN users u ON a.user_id = u.id WHERE a.id = ?";
        }
        
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
                details.put("application", app);
                
                // Get documents
                DocumentDAO documentDAO = new DocumentDAO();
                details.put("documents", documentDAO.getDocumentsByApplicationId(appId));
                
                // Get timeline
                ApplicationDAO applicationDAO = new ApplicationDAO();
                details.put("timeline", applicationDAO.getApplicationTimeline(appId));
                
                // Add permissions
                Map<String, Boolean> permissions = new HashMap<>();
                permissions.put("canChangeStatus", admin.canChangeApplicationStatus());
                permissions.put("canVerifyDocuments", admin.canVerifyDocuments());
                permissions.put("canExportData", admin.canExportData());
                permissions.put("canManageUsers", admin.canManageUsers());
                details.put("permissions", permissions);
                
                details.put("success", true);
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
            conn.setAutoCommit(false);
            
            // First delete related timeline entries
            String deleteTimelineSql = "DELETE FROM application_timeline WHERE application_id = ?";
            ps = conn.prepareStatement(deleteTimelineSql);
            ps.setInt(1, appId);
            ps.executeUpdate();
            ps.close();
            
            // Delete related documents
            String deleteDocsSql = "DELETE FROM documents WHERE application_id = ?";
            ps = conn.prepareStatement(deleteDocsSql);
            ps.setInt(1, appId);
            ps.executeUpdate();
            ps.close();
            
            // Delete the application
            String deleteAppSql = "DELETE FROM applications WHERE id = ?";
            ps = conn.prepareStatement(deleteAppSql);
            ps.setInt(1, appId);
            
            int deleted = ps.executeUpdate();
            
            if (deleted > 0) {
                conn.commit();
                return true;
            }
            
            conn.rollback();
            return false;
            
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
     * Get applications pending review (for reviewers)
     */
    public List<Map<String, Object>> getPendingReviews(Admin admin, int limit) {
        if (!admin.canReviewApplications()) {
            return new ArrayList<>();
        }
        
        List<Map<String, Object>> applications = new ArrayList<>();
        String sql = "SELECT a.id, a.reference_number, a.application_status, " +
                     "u.first_name, u.last_name, u.email, " +
                     "a.created_at, a.submission_date, " +
                     "COUNT(d.id) as total_documents, " +
                     "SUM(CASE WHEN d.upload_status = 'verified' THEN 1 ELSE 0 END) as verified_documents " +
                     "FROM applications a " +
                     "JOIN users u ON a.user_id = u.id " +
                     "LEFT JOIN documents d ON a.id = d.application_id " +
                     "WHERE a.application_status IN ('submitted', 'under_review') " +
                     "GROUP BY a.id " +
                     "ORDER BY a.submission_date ASC LIMIT ?";
        
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
                app.put("applicantName", rs.getString("first_name") + " " + rs.getString("last_name"));
                app.put("email", rs.getString("email"));
                app.put("status", rs.getString("application_status"));
                app.put("submittedAt", rs.getTimestamp("submission_date"));
                app.put("totalDocuments", rs.getInt("total_documents"));
                app.put("verifiedDocuments", rs.getInt("verified_documents"));
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
     * Get review statistics for reviewer dashboard
     */
    public Map<String, Object> getReviewerStats(Admin admin) {
        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT " +
                     "(SELECT COUNT(*) FROM applications WHERE application_status = 'submitted') as pending_review, " +
                     "(SELECT COUNT(*) FROM applications WHERE application_status = 'under_review') as under_review, " +
                     "(SELECT COUNT(*) FROM applications WHERE application_status = 'interview') as for_interview, " +
                     "(SELECT COUNT(*) FROM documents WHERE upload_status = 'pending') as pending_documents, " +
                     "(SELECT COUNT(*) FROM applications WHERE application_status = 'approved') as approved, " +
                     "(SELECT COUNT(*) FROM applications WHERE application_status = 'declined') as declined";
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                stats.put("pendingReview", rs.getInt("pending_review"));
                stats.put("underReview", rs.getInt("under_review"));
                stats.put("forInterview", rs.getInt("for_interview"));
                stats.put("pendingDocuments", rs.getInt("pending_documents"));
                stats.put("approved", rs.getInt("approved"));
                stats.put("declined", rs.getInt("declined"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.closeConnection(conn, ps, rs);
        }
        return stats;
    }
    
    /**
     * Update application status with role check
     */
    public boolean updateApplicationStatus(Admin admin, int appId, String status, String notes, int adminId) {
        if (!admin.canChangeApplicationStatus()) {
            return false;
        }
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String oldStatus = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);
            
            // Get old status
            String getSql = "SELECT application_status FROM applications WHERE id = ?";
            ps = conn.prepareStatement(getSql);
            ps.setInt(1, appId);
            rs = ps.executeQuery();
            if (rs.next()) {
                oldStatus = rs.getString("application_status");
            }
            rs.close();
            ps.close();
            
            // Update status
            String updateSql = "UPDATE applications SET application_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            ps = conn.prepareStatement(updateSql);
            ps.setString(1, status);
            ps.setInt(2, appId);
            
            int updated = ps.executeUpdate();
            
            if (updated > 0) {
                // Add timeline entry
                ApplicationDAO applicationDAO = new ApplicationDAO();
                applicationDAO.addTimelineEntry(appId, adminId, "STATUS_UPDATE", 
                    notes, oldStatus, status);
                
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
    
    /**
     * Verify document with role check
     */
    public boolean verifyDocument(Admin admin, int docId, int appId, String status, String rejectionReason, int adminId) {
        if (!admin.canVerifyDocuments()) {
            return false;
        }
        
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DatabaseConfig.getConnection();
            String sql = "UPDATE documents SET upload_status = ?, verified_at = CURRENT_TIMESTAMP, " +
                        "rejection_reason = ? WHERE id = ? AND application_id = ?";
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setString(2, rejectionReason);
            ps.setInt(3, docId);
            ps.setInt(4, appId);
            
            int updated = ps.executeUpdate();
            
            if (updated > 0) {
                // Add timeline entry
                ApplicationDAO applicationDAO = new ApplicationDAO();
                applicationDAO.addTimelineEntry(appId, adminId,
                    "DOCUMENT_" + status.toUpperCase(), 
                    "Document " + (status.equals("verified") ? "verified" : "rejected") + 
                    (rejectionReason != null ? ": " + rejectionReason : ""),
                    null, null);
                
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseConfig.closeConnection(conn, ps);
        }
        return false;
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
}