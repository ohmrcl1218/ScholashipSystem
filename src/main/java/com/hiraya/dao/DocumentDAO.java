package com.hiraya.dao;

import com.hiraya.model.Document;
import com.hiraya.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DocumentDAO {
    private DatabaseUtil dbUtil;
    
    public DocumentDAO() {
        this.dbUtil = DatabaseUtil.getInstance();
    }
    
    // Save document info
    public Document saveDocument(Document document) {
        System.out.println("\n===== SAVING DOCUMENT TO DATABASE =====");
        System.out.println("Application ID: " + document.getApplicationId());
        System.out.println("User ID: " + document.getUserId());
        System.out.println("Document Type: " + document.getDocumentType());
        System.out.println("File Name: " + document.getFileName());
        System.out.println("File Size: " + document.getFileSize() + " bytes");
        System.out.println("Mime Type: " + document.getMimeType());
        
        String sql = "INSERT INTO documents (application_id, user_id, document_type, file_name, " +
                    "file_path, file_size, mime_type, upload_status, uploaded_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'uploaded', CURRENT_TIMESTAMP)";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dbUtil.getConnection();
            System.out.println("✅ Got database connection");
            
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setInt(1, document.getApplicationId());
            stmt.setInt(2, document.getUserId());
            stmt.setString(3, document.getDocumentType());
            stmt.setString(4, document.getFileName());
            stmt.setString(5, document.getFilePath());
            stmt.setLong(6, document.getFileSize());
            stmt.setString(7, document.getMimeType());
            
            System.out.println("Executing INSERT...");
            int affectedRows = stmt.executeUpdate();
            System.out.println("Insert affected rows: " + affectedRows);
            
            if (affectedRows > 0) {
                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    document.setId(rs.getInt(1));
                    System.out.println("✅ Document saved with ID: " + document.getId());
                }
                return document;
            } else {
                System.err.println("❌ No rows affected in INSERT");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ SQL Error in saveDocument:");
            System.err.println("   Error Code: " + e.getErrorCode());
            System.err.println("   SQL State: " + e.getSQLState());
            System.err.println("   Message: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            DatabaseUtil.closeConnection(conn);
        }
        
        System.err.println("❌ Failed to save document");
        System.out.println("===== END SAVE DOCUMENT =====\n");
        return null;
    }
    
    // Get documents by application ID
    public List<Document> getDocumentsByApplicationId(int applicationId) {
        List<Document> documents = new ArrayList<>();
        String sql = "SELECT * FROM documents WHERE application_id = ? ORDER BY document_type";
        
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, applicationId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                documents.add(mapResultSetToDocument(rs));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return documents;
    }
    
    // Check if all required documents are uploaded
    public boolean hasAllRequiredDocuments(int applicationId) {
        String[] requiredDocs = {
            "proof_enrollment",  // Must match exactly what's in the HTML
            "picture",
            "valid_id",
            "report_card",
            "birth_cert",
            "moral_cert",
            "health_cert",
            "indigency_cert",
            "income_cert",
            "reco_letter"
        };
        
        String sql = "SELECT COUNT(DISTINCT document_type) FROM documents " +
                    "WHERE application_id = ? AND document_type IN (?,?,?,?,?,?,?,?,?,?) AND upload_status = 'uploaded'";
        
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, applicationId);
            for (int i = 0; i < requiredDocs.length; i++) {
                stmt.setString(i + 2, requiredDocs[i]);
            }
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("Uploaded documents count: " + count + " / " + requiredDocs.length);
                return count == requiredDocs.length;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    // Map ResultSet to Document object
    private Document mapResultSetToDocument(ResultSet rs) throws SQLException {
        Document doc = new Document();
        
        doc.setId(rs.getInt("id"));
        doc.setApplicationId(rs.getInt("application_id"));
        doc.setUserId(rs.getInt("user_id"));
        doc.setDocumentType(rs.getString("document_type"));
        doc.setFileName(rs.getString("file_name"));
        doc.setFilePath(rs.getString("file_path"));
        doc.setFileSize(rs.getLong("file_size"));
        doc.setMimeType(rs.getString("mime_type"));
        doc.setUploadStatus(rs.getString("upload_status"));
        doc.setUploadedAt(rs.getTimestamp("uploaded_at"));
        doc.setVerifiedAt(rs.getTimestamp("verified_at"));
        doc.setRejectionReason(rs.getString("rejection_reason"));
        
        return doc;
    }
}