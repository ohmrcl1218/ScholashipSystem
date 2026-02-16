package com.hiraya.dao;

import com.hiraya.config.DatabaseConfig;
import com.hiraya.model.Document;
import com.hiraya.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentDAO {
    private DatabaseUtil dbUtil;
    
    public DocumentDAO() {
        this.dbUtil = DatabaseUtil.getInstance();
    }
    
    // ... (keep all your existing methods) ...
    
    /**
     * Get documents with status for admin dashboard
     * Returns both raw documents and formatted document status
     * This matches the student dashboard format
     */
    public Map<String, Object> getDocumentsWithStatus(int applicationId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get all documents using existing method
            List<Document> documents = getDocumentsByApplicationId(applicationId);
            
            // Convert to map format with BOTH naming conventions
            List<Map<String, Object>> documentMaps = new ArrayList<>();
            for (Document doc : documents) {
                Map<String, Object> docMap = new HashMap<>();
                
                // Add both camelCase and snake_case for compatibility
                docMap.put("id", doc.getId());
                docMap.put("applicationId", doc.getApplicationId());
                docMap.put("userId", doc.getUserId());
                
                // Document type - both formats
                docMap.put("documentType", doc.getDocumentType());
                docMap.put("document_type", doc.getDocumentType());
                
                // File name - both formats
                docMap.put("fileName", doc.getFileName());
                docMap.put("file_name", doc.getFileName());
                
                // File path
                docMap.put("filePath", doc.getFilePath());
                docMap.put("file_path", doc.getFilePath());
                
                // File size
                docMap.put("fileSize", doc.getFileSize());
                docMap.put("file_size", doc.getFileSize());
                
                // Mime type
                docMap.put("mimeType", doc.getMimeType());
                docMap.put("mime_type", doc.getMimeType());
                
                // Upload status - both formats
                docMap.put("uploadStatus", doc.getUploadStatus());
                docMap.put("upload_status", doc.getUploadStatus());
                
                // Timestamps - both formats
                docMap.put("uploadedAt", doc.getUploadedAt());
                docMap.put("uploaded_at", doc.getUploadedAt());
                docMap.put("verifiedAt", doc.getVerifiedAt());
                docMap.put("verified_at", doc.getVerifiedAt());
                docMap.put("createdAt", doc.getCreatedAt());
                docMap.put("created_at", doc.getCreatedAt());
                docMap.put("updatedAt", doc.getUpdatedAt());
                docMap.put("updated_at", doc.getUpdatedAt());
                
                // Other fields
                docMap.put("verifiedBy", doc.getVerifiedBy());
                docMap.put("verified_by", doc.getVerifiedBy());
                docMap.put("rejectionReason", doc.getRejectionReason());
                docMap.put("rejection_reason", doc.getRejectionReason());
                
                documentMaps.add(docMap);
            }
            
            // Build document status summary
            Map<String, Object> documentStatus = buildDocumentStatus(documents);
            
            response.put("success", true);
            response.put("documents", documentMaps);
            response.put("documentStatus", documentStatus);
            
            System.out.println("✅ DocumentDAO: Returning " + documentMaps.size() + 
                             " documents with status for application " + applicationId);
            
        } catch (Exception e) {
            System.err.println("❌ Error in getDocumentsWithStatus: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Failed to fetch documents: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Build document status summary matching student dashboard format
     */
    private Map<String, Object> buildDocumentStatus(List<Document> documents) {
        Map<String, Object> status = new HashMap<>();
        
        // Define all required documents with their display info
        Map<String, DocumentInfo> requiredDocs = new HashMap<>();
        requiredDocs.put(Document.PROOF_ENROLLMENT, new DocumentInfo("Proof of Enrollment", "🎓", true));
        requiredDocs.put(Document.PICTURE, new DocumentInfo("2x2 Picture", "📸", true));
        requiredDocs.put(Document.VALID_ID, new DocumentInfo("Valid ID", "🆔", true));
        requiredDocs.put(Document.REPORT_CARD, new DocumentInfo("Report Card", "📊", true));
        requiredDocs.put(Document.BIRTH_CERT, new DocumentInfo("Birth Certificate", "📄", true));
        requiredDocs.put(Document.MORAL_CERT, new DocumentInfo("Good Moral Certificate", "✅", true));
        requiredDocs.put(Document.HEALTH_CERT, new DocumentInfo("Health Certificate", "🏥", true));
        requiredDocs.put(Document.INDIGENCY_CERT, new DocumentInfo("Certificate of Indigency", "📋", false));
        requiredDocs.put(Document.INCOME_CERT, new DocumentInfo("Income Certificate", "💰", false));
        requiredDocs.put(Document.RECOMMENDATION_LETTER, new DocumentInfo("Recommendation Letter", "📝", false));
        
        // Create a map of uploaded documents by type
        Map<String, Document> uploadedDocs = new HashMap<>();
        for (Document doc : documents) {
            if ("uploaded".equals(doc.getUploadStatus()) || "verified".equals(doc.getUploadStatus())) {
                uploadedDocs.put(doc.getDocumentType(), doc);
            }
        }
        
        // Count verified, pending, missing
        int verified = 0;
        int pending = 0;
        int missing = 0;
        int total = requiredDocs.size();
        
        List<Map<String, Object>> formattedDocs = new ArrayList<>();
        
        // Process each required document type
        for (Map.Entry<String, DocumentInfo> entry : requiredDocs.entrySet()) {
            String docType = entry.getKey();
            DocumentInfo info = entry.getValue();
            
            Map<String, Object> formattedDoc = new HashMap<>();
            formattedDoc.put("name", info.name);
            formattedDoc.put("icon", info.icon);
            formattedDoc.put("required", info.required);
            
            if (uploadedDocs.containsKey(docType)) {
                Document uploadedDoc = uploadedDocs.get(docType);
                
                formattedDoc.put("id", uploadedDoc.getId());
                formattedDoc.put("status", "verified");
                formattedDoc.put("statusText", "Verified");
                formattedDoc.put("fileName", uploadedDoc.getFileName());
                formattedDoc.put("uploadedAt", uploadedDoc.getUploadedAt());
                
                verified++;
            } else {
                formattedDoc.put("status", "missing");
                formattedDoc.put("statusText", "Missing");
                formattedDoc.put("fileName", null);
                missing++;
            }
            
            formattedDocs.add(formattedDoc);
        }
        
        // Calculate completion percentage
        int completionPercentage = total > 0 ? (int) ((verified / (double) total) * 100) : 0;
        
        status.put("verified", verified);
        status.put("pending", pending);
        status.put("missing", missing);
        status.put("total", total);
        status.put("completionPercentage", completionPercentage);
        status.put("documents", formattedDocs);
        
        System.out.println("📊 Document Status: " + verified + " verified, " + 
                         pending + " pending, " + missing + " missing");
        
        return status;
    }
    
    /**
     * Helper class to store document display information
     */
    private static class DocumentInfo {
        String name;
        String icon;
        boolean required;
        
        DocumentInfo(String name, String icon, boolean required) {
            this.name = name;
            this.icon = icon;
            this.required = required;
        }
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
            System.out.println("DAO: Retrieved " + documents.size() + " documents for application " + applicationId);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return documents;
    }
    
    // Check if all required documents are uploaded
    public boolean hasAllRequiredDocuments(int applicationId) {
        String[] requiredDocs = {
            "proof_enrollment",
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

    /**
     * Get document by ID
     */
    public Document getDocumentById(int docId) {
        String sql = "SELECT * FROM documents WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, docId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToDocument(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }

    /**
     * Verify document
     */
    public boolean verifyDocument(int docId, String adminId) {
        String sql = "UPDATE documents SET upload_status = 'verified', verified_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, docId);
            
            int updated = ps.executeUpdate();
            return updated > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Map ResultSet to Document object
     */
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

        // IMPORTANT: verified_by can be NULL
        Integer verifiedBy = (Integer) rs.getObject("verified_by");
        doc.setVerifiedBy(verifiedBy);

        doc.setRejectionReason(rs.getString("rejection_reason"));
        doc.setCreatedAt(rs.getTimestamp("created_at"));
        doc.setUpdatedAt(rs.getTimestamp("updated_at"));

        return doc;
    }
}