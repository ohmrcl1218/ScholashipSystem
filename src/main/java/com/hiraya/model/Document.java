package com.hiraya.model;

import java.sql.Timestamp;

public class Document {
    private int id;
    private int applicationId;
    private int userId;
    private String documentType;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private String uploadStatus;
    private Timestamp uploadedAt;
    private Timestamp verifiedAt;
    private String rejectionReason;
    
    // Document types - MUST MATCH HTML data-doc-type attributes EXACTLY
    public static final String PROOF_ENROLLMENT = "proof_enrollment";
    public static final String PICTURE = "picture";
    public static final String VALID_ID = "valid_id";
    public static final String REPORT_CARD = "report_card";
    public static final String BIRTH_CERT = "birth_cert";
    public static final String MORAL_CERT = "moral_cert";
    public static final String HEALTH_CERT = "health_cert";
    public static final String INDIGENCY_CERT = "indigency_cert";
    public static final String INCOME_CERT = "income_cert";
    public static final String RECOMMENDATION_LETTER = "reco_letter";
    
    public Document() {}
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    
    public String getUploadStatus() { return uploadStatus; }
    public void setUploadStatus(String uploadStatus) { this.uploadStatus = uploadStatus; }
    
    public Timestamp getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Timestamp uploadedAt) { this.uploadedAt = uploadedAt; }
    
    public Timestamp getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Timestamp verifiedAt) { this.verifiedAt = verifiedAt; }
    
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
