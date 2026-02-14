package com.hiraya.dao;

import com.hiraya.model.Application;
import com.hiraya.util.DatabaseUtil;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationDAO {
    private DatabaseUtil dbUtil;
    
    public ApplicationDAO() {
        this.dbUtil = DatabaseUtil.getInstance();
        System.out.println("✅ ApplicationDAO initialized");
    }
    
    // ========== SAVE DRAFT - COMPLETE FIXED VERSION ==========
 // ========== SAVE DRAFT - COMPLETE FIXED VERSION WITH ALL FIELDS ==========
    public Application saveDraft(Application application) {
        System.out.println("\n========== SAVE DRAFT ==========");
        System.out.println("User ID: " + application.getUserId());
        System.out.println("First Name: " + application.getFirstName());
        System.out.println("Last Name: " + application.getLastName());
        System.out.println("Email: " + application.getEmail());
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbUtil.getConnection();
            System.out.println("✅ Got fresh connection: " + conn.hashCode());
            
            // Check for existing draft
            Application existingDraft = getApplicationByUserIdAndStatus(application.getUserId(), "draft");
            
            if (existingDraft != null) {
                // UPDATE existing draft - NOW WITH ALL FIELDS!
                System.out.println("📝 Updating existing draft ID: " + existingDraft.getId());
                application.setId(existingDraft.getId());
                
                String sql = "UPDATE applications SET " +
                    // Personal Info
                    "first_name = ?, middle_name = ?, last_name = ?, sex = ?, birthdate = ?, age = ?, " +
                    "place_of_birth = ?, height = ?, weight = ?, mobile_number = ?, email = ?, facebook_url = ?, " +
                    
                    // Present Address
                    "present_region = ?, present_province = ?, present_municipality = ?, present_barangay = ?, " +
                    "present_house_number = ?, present_street = ?, present_zip_code = ?, " +
                    
                    // Permanent Address
                    "permanent_region = ?, permanent_province = ?, permanent_municipality = ?, permanent_barangay = ?, " +
                    "permanent_house_number = ?, permanent_street = ?, permanent_zip_code = ?, " +
                    
                    // Academic - JHS
                    "jhs_name = ?, jhs_school_id = ?, jhs_type = ?, " +
                    
                    // Academic - SHS
                    "shs_name = ?, shs_school_id = ?, shs_type = ?, " +
                    "track = ?, strand = ?, grade_12_gwa = ?, honors_received = ?, " +
                    
                    // College Choices
                    "college_first = ?, college_second = ?, college_third = ?, " +
                    "program_first = ?, program_second = ?, program_third = ?, " +
                    
                    // Essay
                    "essay = ?, " +
                    
                    // Status and timestamp
                    "application_status = ?, last_saved = CURRENT_TIMESTAMP " +
                    "WHERE id = ? AND user_id = ?";
                
                stmt = conn.prepareStatement(sql);
                
                int index = 1;
                
                // Personal Info
                stmt.setString(index++, application.getFirstName());
                stmt.setString(index++, application.getMiddleName());
                stmt.setString(index++, application.getLastName());
                stmt.setString(index++, application.getSex());
                stmt.setDate(index++, application.getBirthdate());
                setInt(stmt, index++, application.getAge());
                stmt.setString(index++, application.getPlaceOfBirth());
                setDouble(stmt, index++, application.getHeight());
                setDouble(stmt, index++, application.getWeight());
                stmt.setString(index++, application.getMobileNumber());
                stmt.setString(index++, application.getEmail());
                stmt.setString(index++, application.getFacebookUrl());
                
                // Present Address
                stmt.setString(index++, application.getPresentRegion());
                stmt.setString(index++, application.getPresentProvince());
                stmt.setString(index++, application.getPresentMunicipality());
                stmt.setString(index++, application.getPresentBarangay());
                stmt.setString(index++, application.getPresentHouseNumber());
                stmt.setString(index++, application.getPresentStreet());
                stmt.setString(index++, application.getPresentZipCode());
                
                // Permanent Address
                stmt.setString(index++, application.getPermanentRegion());
                stmt.setString(index++, application.getPermanentProvince());
                stmt.setString(index++, application.getPermanentMunicipality());
                stmt.setString(index++, application.getPermanentBarangay());
                stmt.setString(index++, application.getPermanentHouseNumber());
                stmt.setString(index++, application.getPermanentStreet());
                stmt.setString(index++, application.getPermanentZipCode());
                
                // Academic - JHS
                stmt.setString(index++, application.getJhsName());
                stmt.setString(index++, application.getJhsSchoolId());
                stmt.setString(index++, application.getJhsType());
                
                // Academic - SHS
                stmt.setString(index++, application.getShsName());
                stmt.setString(index++, application.getShsSchoolId());
                stmt.setString(index++, application.getShsType());
                stmt.setString(index++, application.getTrack());
                stmt.setString(index++, application.getStrand());
                setDouble(stmt, index++, application.getGrade12Gwa());
                stmt.setString(index++, application.getHonorsReceived());
                
                // College Choices
                stmt.setString(index++, application.getCollegeFirst());
                stmt.setString(index++, application.getCollegeSecond());
                stmt.setString(index++, application.getCollegeThird());
                stmt.setString(index++, application.getProgramFirst());
                stmt.setString(index++, application.getProgramSecond());
                stmt.setString(index++, application.getProgramThird());
                
                // Essay
                stmt.setString(index++, application.getEssay());
                
                // Status
                stmt.setString(index++, "draft");
                
                // WHERE clause
                stmt.setInt(index++, application.getId());
                stmt.setInt(index++, application.getUserId());
                
                int rows = stmt.executeUpdate();
                System.out.println("UPDATE affected rows: " + rows);
                
                if (rows > 0) {
                    System.out.println("✅ UPDATE successful!");
                    return application;
                }
            } else {
                // INSERT new draft - NOW WITH ALL FIELDS!
                System.out.println("📝 Creating new draft application with all fields");
                
                String sql = "INSERT INTO applications (" +
                    "user_id, application_status, last_saved, " +
                    
                    // Personal Info
                    "first_name, middle_name, last_name, sex, birthdate, age, place_of_birth, height, weight, " +
                    "mobile_number, email, facebook_url, " +
                    
                    // Present Address
                    "present_region, present_province, present_municipality, present_barangay, " +
                    "present_house_number, present_street, present_zip_code, " +
                    
                    // Permanent Address
                    "permanent_region, permanent_province, permanent_municipality, permanent_barangay, " +
                    "permanent_house_number, permanent_street, permanent_zip_code, " +
                    
                    // Academic - JHS
                    "jhs_name, jhs_school_id, jhs_type, " +
                    
                    // Academic - SHS
                    "shs_name, shs_school_id, shs_type, track, strand, grade_12_gwa, honors_received, " +
                    
                    // College Choices
                    "college_first, college_second, college_third, " +
                    "program_first, program_second, program_third, " +
                    
                    // Essay
                    "essay) " +
                    
                    "VALUES (" +
                    "?, 'draft', CURRENT_TIMESTAMP, " +  // user_id, status, last_saved
                    
                    // Personal Info (13 placeholders)
                    "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +  // 13 personal fields
                    
                    // Present Address (7 placeholders)
                    "?, ?, ?, ?, ?, ?, ?, " +  // 7 present address fields
                    
                    // Permanent Address (7 placeholders)
                    "?, ?, ?, ?, ?, ?, ?, " +  // 7 permanent address fields
                    
                    // JHS (3 placeholders)
                    "?, ?, ?, " +  // 3 JHS fields
                    
                    // SHS (7 placeholders)
                    "?, ?, ?, ?, ?, ?, ?, " +  // 7 SHS fields
                    
                    // College Choices (6 placeholders)
                    "?, ?, ?, ?, ?, ?, " +  // 6 college/program fields
                    
                    // Essay (1 placeholder)
                    "?)";  // 1 essay field
                
                stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                
                int index = 1;
                
                // user_id
                stmt.setInt(index++, application.getUserId());
                
                // Personal Info (13 fields)
                stmt.setString(index++, application.getFirstName());
                stmt.setString(index++, application.getMiddleName());
                stmt.setString(index++, application.getLastName());
                stmt.setString(index++, application.getSex());
                stmt.setDate(index++, application.getBirthdate());
                setInt(stmt, index++, application.getAge());
                stmt.setString(index++, application.getPlaceOfBirth());
                setDouble(stmt, index++, application.getHeight());
                setDouble(stmt, index++, application.getWeight());
                stmt.setString(index++, application.getMobileNumber());
                stmt.setString(index++, application.getEmail());
                stmt.setString(index++, application.getFacebookUrl());
                
                // Present Address (7 fields)
                stmt.setString(index++, application.getPresentRegion());
                stmt.setString(index++, application.getPresentProvince());
                stmt.setString(index++, application.getPresentMunicipality());
                stmt.setString(index++, application.getPresentBarangay());
                stmt.setString(index++, application.getPresentHouseNumber());
                stmt.setString(index++, application.getPresentStreet());
                stmt.setString(index++, application.getPresentZipCode());
                
                // Permanent Address (7 fields)
                stmt.setString(index++, application.getPermanentRegion());
                stmt.setString(index++, application.getPermanentProvince());
                stmt.setString(index++, application.getPermanentMunicipality());
                stmt.setString(index++, application.getPermanentBarangay());
                stmt.setString(index++, application.getPermanentHouseNumber());
                stmt.setString(index++, application.getPermanentStreet());
                stmt.setString(index++, application.getPermanentZipCode());
                
                // JHS (3 fields)
                stmt.setString(index++, application.getJhsName());
                stmt.setString(index++, application.getJhsSchoolId());
                stmt.setString(index++, application.getJhsType());
                
                // SHS (7 fields)
                stmt.setString(index++, application.getShsName());
                stmt.setString(index++, application.getShsSchoolId());
                stmt.setString(index++, application.getShsType());
                stmt.setString(index++, application.getTrack());
                stmt.setString(index++, application.getStrand());
                setDouble(stmt, index++, application.getGrade12Gwa());
                stmt.setString(index++, application.getHonorsReceived());
                
                // College Choices (6 fields)
                stmt.setString(index++, application.getCollegeFirst());
                stmt.setString(index++, application.getCollegeSecond());
                stmt.setString(index++, application.getCollegeThird());
                stmt.setString(index++, application.getProgramFirst());
                stmt.setString(index++, application.getProgramSecond());
                stmt.setString(index++, application.getProgramThird());
                
                // Essay (1 field)
                stmt.setString(index++, application.getEssay());
                
                System.out.println("Executing INSERT with " + (index-1) + " parameters");
                
                int rows = stmt.executeUpdate();
                System.out.println("INSERT affected rows: " + rows);
                
                if (rows > 0) {
                    rs = stmt.getGeneratedKeys();
                    if (rs.next()) {
                        int newId = rs.getInt(1);
                        application.setId(newId);
                        System.out.println("✅ New Application ID: " + newId);
                    }
                    application.setApplicationStatus("draft");
                    System.out.println("✅ INSERT successful!");
                    return application;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("\n❌ SQL ERROR in saveDraft:");
            System.err.println("   Error Code: " + e.getErrorCode());
            System.err.println("   SQL State: " + e.getSQLState());
            System.err.println("   Message: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            DatabaseUtil.closeConnection(conn);
        }
        
        System.out.println("========== END SAVE DRAFT ==========\n");
        return null;
    }
    
    // ========== GET APPLICATION BY USER ID AND STATUS ==========
    public Application getApplicationByUserIdAndStatus(int userId, String status) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbUtil.getConnection();
            String sql = "SELECT * FROM applications WHERE user_id = ? AND application_status = ? ORDER BY id DESC LIMIT 1";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setString(2, status);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToApplication(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            // DON'T close connection here - let the caller handle it
        }
        
        return null;
    }
    
    // ========== GET APPLICATION BY USER ID ==========
    public Application getApplicationByUserId(int userId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbUtil.getConnection();
            String sql = "SELECT * FROM applications WHERE user_id = ? ORDER BY id DESC LIMIT 1";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToApplication(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            DatabaseUtil.closeConnection(conn);
        }
        
        return null;
    }
    
    // ========== GET APPLICATION BY ID ==========
    public Application getApplicationById(int id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbUtil.getConnection();
            String sql = "SELECT * FROM applications WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToApplication(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            DatabaseUtil.closeConnection(conn);
        }
        
        return null;
    }
    
    // ========== CHECK IF USER HAS DRAFT ==========
    public boolean hasDraft(int userId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbUtil.getConnection();
            String sql = "SELECT COUNT(*) FROM applications WHERE user_id = ? AND application_status = 'draft'";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            DatabaseUtil.closeConnection(conn);
        }
        
        return false;
    }
    
    // ========== SUBMIT APPLICATION ==========
    public Application submitApplication(Application application) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dbUtil.getConnection();
            String sql = "UPDATE applications SET application_status = 'submitted', submission_date = ?, " +
                        "reference_number = ? WHERE id = ? AND user_id = ? AND application_status = 'draft'";
            
            stmt = conn.prepareStatement(sql);
            
            Timestamp now = DatabaseUtil.getCurrentTimestamp();
            String referenceNumber = generateReferenceNumber();
            
            stmt.setTimestamp(1, now);
            stmt.setString(2, referenceNumber);
            stmt.setInt(3, application.getId());
            stmt.setInt(4, application.getUserId());
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                application.setApplicationStatus("submitted");
                application.setSubmissionDate(now);
                application.setReferenceNumber(referenceNumber);
                System.out.println("✅ Application submitted: " + referenceNumber);
                return application;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            DatabaseUtil.closeConnection(conn);
        }
        
        return null;
    }
    
    // ========== DELETE DRAFT ==========
    public boolean deleteDraft(int applicationId, int userId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dbUtil.getConnection();
            String sql = "DELETE FROM applications WHERE id = ? AND user_id = ? AND application_status = 'draft'";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, applicationId);
            stmt.setInt(2, userId);
            
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            DatabaseUtil.closeConnection(conn);
        }
        
        return false;
    }
    
    // ========== GET APPLICATION TIMELINE ==========
    public List<Map<String, Object>> getApplicationTimeline(int applicationId) {
        List<Map<String, Object>> timeline = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbUtil.getConnection();
            String sql = "SELECT * FROM application_timeline WHERE application_id = ? ORDER BY created_at DESC";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, applicationId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("id", rs.getInt("id"));
                entry.put("action", rs.getString("action"));
                entry.put("description", rs.getString("description"));
                entry.put("statusBefore", rs.getString("status_before"));
                entry.put("statusAfter", rs.getString("status_after"));
                entry.put("createdAt", rs.getTimestamp("created_at"));
                entry.put("createdAtFormatted", formatTimestamp(rs.getTimestamp("created_at")));
                timeline.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            DatabaseUtil.closeConnection(conn);
        }
        
        return timeline;
    }
    
    // ========== ADD TIMELINE ENTRY ==========
    public void addTimelineEntry(int applicationId, int userId, String action, 
                                String description, String statusBefore, String statusAfter) {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dbUtil.getConnection();
            String sql = "INSERT INTO application_timeline (application_id, user_id, action, " +
                        "description, status_before, status_after, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(sql);
            
            stmt.setInt(1, applicationId);
            stmt.setInt(2, userId);
            stmt.setString(3, action);
            stmt.setString(4, description);
            stmt.setString(5, statusBefore);
            stmt.setString(6, statusAfter);
            stmt.setTimestamp(7, DatabaseUtil.getCurrentTimestamp());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            DatabaseUtil.closeConnection(conn);
        }
    }
    
    // ========== HELPER METHODS ==========
    private void setInt(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value != null) {
            stmt.setInt(index, value);
        } else {
            stmt.setNull(index, Types.INTEGER);
        }
    }
    
    private void setDouble(PreparedStatement stmt, int index, Double value) throws SQLException {
        if (value != null) {
            stmt.setDouble(index, value);
        } else {
            stmt.setNull(index, Types.DOUBLE);
        }
    }
    
    private String generateReferenceNumber() {
        String year = String.valueOf(java.time.Year.now().getValue());
        String randomNum = String.format("%05d", (int)(Math.random() * 100000));
        return "HF-" + year + "-" + randomNum;
    }
    
    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(timestamp);
    }
    
    // ========== MAP RESULTSET TO APPLICATION ==========
    private Application mapResultSetToApplication(ResultSet rs) throws SQLException {
        Application app = new Application();
        
        app.setId(rs.getInt("id"));
        app.setUserId(rs.getInt("user_id"));
        app.setReferenceNumber(rs.getString("reference_number"));
        
        // Personal Information
        app.setFirstName(rs.getString("first_name"));
        app.setMiddleName(rs.getString("middle_name"));
        app.setLastName(rs.getString("last_name"));
        app.setSex(rs.getString("sex"));
        app.setBirthdate(rs.getDate("birthdate"));
        app.setAge(rs.getInt("age"));
        if (rs.wasNull()) app.setAge(null);
        app.setPlaceOfBirth(rs.getString("place_of_birth"));
        app.setHeight(rs.getDouble("height"));
        if (rs.wasNull()) app.setHeight(null);
        app.setWeight(rs.getDouble("weight"));
        if (rs.wasNull()) app.setWeight(null);
        app.setMobileNumber(rs.getString("mobile_number"));
        app.setEmail(rs.getString("email"));
        app.setFacebookUrl(rs.getString("facebook_url"));
        
        // Present Address
        app.setPresentRegion(rs.getString("present_region"));
        app.setPresentProvince(rs.getString("present_province"));
        app.setPresentMunicipality(rs.getString("present_municipality"));
        app.setPresentBarangay(rs.getString("present_barangay"));
        app.setPresentHouseNumber(rs.getString("present_house_number"));
        app.setPresentStreet(rs.getString("present_street"));
        app.setPresentZipCode(rs.getString("present_zip_code"));
        
        // Permanent Address
        app.setPermanentRegion(rs.getString("permanent_region"));
        app.setPermanentProvince(rs.getString("permanent_province"));
        app.setPermanentMunicipality(rs.getString("permanent_municipality"));
        app.setPermanentBarangay(rs.getString("permanent_barangay"));
        app.setPermanentHouseNumber(rs.getString("permanent_house_number"));
        app.setPermanentStreet(rs.getString("permanent_street"));
        app.setPermanentZipCode(rs.getString("permanent_zip_code"));
        
        // Academic - JHS
        app.setJhsName(rs.getString("jhs_name"));
        app.setJhsSchoolId(rs.getString("jhs_school_id"));
        app.setJhsType(rs.getString("jhs_type"));
        
        // Academic - SHS
        app.setShsName(rs.getString("shs_name"));
        app.setShsSchoolId(rs.getString("shs_school_id"));
        app.setShsType(rs.getString("shs_type"));
        app.setTrack(rs.getString("track"));
        app.setStrand(rs.getString("strand"));
        app.setGrade12Gwa(rs.getDouble("grade_12_gwa"));
        if (rs.wasNull()) app.setGrade12Gwa(null);
        app.setHonorsReceived(rs.getString("honors_received"));
        
        // College Choices
        app.setCollegeFirst(rs.getString("college_first"));
        app.setCollegeSecond(rs.getString("college_second"));
        app.setCollegeThird(rs.getString("college_third"));
        app.setProgramFirst(rs.getString("program_first"));
        app.setProgramSecond(rs.getString("program_second"));
        app.setProgramThird(rs.getString("program_third"));
        
        // Essay
        app.setEssay(rs.getString("essay"));
        
        // Status
        app.setApplicationStatus(rs.getString("application_status"));
        app.setSubmissionDate(rs.getTimestamp("submission_date"));
        app.setLastSaved(rs.getTimestamp("last_saved"));
        app.setCreatedAt(rs.getTimestamp("created_at"));
        app.setUpdatedAt(rs.getTimestamp("updated_at"));
        
        return app;
    }
}