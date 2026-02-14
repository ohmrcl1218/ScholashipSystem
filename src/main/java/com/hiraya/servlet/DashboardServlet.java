package com.hiraya.servlet;

import com.hiraya.dao.ApplicationDAO;
import com.hiraya.dao.DocumentDAO;
import com.hiraya.dao.UserDAO;
import com.hiraya.model.Application;
import com.hiraya.model.Document;
import com.hiraya.model.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/dashboard/*")
public class DashboardServlet extends HttpServlet {
    private UserDAO userDAO;
    private ApplicationDAO applicationDAO;
    private DocumentDAO documentDAO;
    private Gson gson;
    
    @Override
    public void init() {
        userDAO = new UserDAO();
        applicationDAO = new ApplicationDAO();
        documentDAO = new DocumentDAO();
        
        // Configure Gson with proper date formatting
        GsonBuilder gsonBuilder = new GsonBuilder();
        
        // Register adapter for java.sql.Date
        gsonBuilder.registerTypeAdapter(Date.class, (JsonSerializer<Date>) (src, typeOfSrc, context) -> {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            return src == null ? null : new JsonPrimitive(formatter.format(src));
        });
        
        // Register adapter for java.sql.Timestamp
        gsonBuilder.registerTypeAdapter(Timestamp.class, (JsonSerializer<Timestamp>) (src, typeOfSrc, context) -> {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return src == null ? null : new JsonPrimitive(formatter.format(src));
        });
        
        // Enable pretty printing for debugging
        gsonBuilder.setPrettyPrinting();
        
        gson = gsonBuilder.create();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        HttpSession session = request.getSession(false);
        
        try {
            // Check authentication
            if (session == null || session.getAttribute("userId") == null) {
                result.put("authenticated", false);
                result.put("message", "Not authenticated");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
            int userId = (int) session.getAttribute("userId");
            
            // Get complete user data with all fields
            User user = userDAO.getUserById(userId);
            if (user == null) {
                result.put("authenticated", false);
                result.put("message", "User not found");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
            String pathInfo = request.getPathInfo();
            
            // Handle specific application request
            if (pathInfo != null && pathInfo.equals("/application")) {
                handleApplicationRequest(request, response, userId, result);
                return;
            }
            
            // Get main dashboard data
            Application application = applicationDAO.getApplicationByUserId(userId);
            
            // Add user data with all fields
            Map<String, Object> userData = getUserDataMap(user);
            result.put("authenticated", true);
            result.put("user", userData);
            result.put("application", application);
            
            // Add application data if exists
            if (application != null) {
                addApplicationData(application, userId, result);
            } else {
                // Add default values when no application exists
                result.put("hasDraft", false);
                result.put("canEdit", true);
                result.put("isSubmitted", false);
                result.put("documentStatus", getDefaultDocumentStatus());
                result.put("timeline", new ArrayList<>());
                result.put("completionPercentage", 0);
                result.put("allDocumentsUploaded", false);
                result.put("isApplicationComplete", false);
            }
            
            // Add session info
            result.put("sessionId", session.getId());
            result.put("lastAccessed", session.getLastAccessedTime());
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("authenticated", false);
            result.put("success", false);
            result.put("message", "An error occurred: " + e.getMessage());
            result.put("error", e.toString());
        }
        
        response.getWriter().write(gson.toJson(result));
    }
    
    /**
     * Handle specific application request
     */
    private void handleApplicationRequest(HttpServletRequest request, HttpServletResponse response, 
                                         int userId, Map<String, Object> result) throws IOException {
        String appId = request.getParameter("id");
        if (appId == null || appId.isEmpty()) {
            result.put("success", false);
            result.put("message", "Application ID is required");
            response.getWriter().write(gson.toJson(result));
            return;
        }
        
        try {
            int applicationId = Integer.parseInt(appId);
            Application application = applicationDAO.getApplicationById(applicationId);
            
            if (application == null) {
                result.put("success", false);
                result.put("message", "Application not found");
            } else if (application.getUserId() != userId) {
                result.put("success", false);
                result.put("message", "Access denied");
            } else {
                // Get all documents for this application
                List<Document> documents = documentDAO.getDocumentsByApplicationId(applicationId);
                
                // Get complete document status with details
                Map<String, Object> documentStatus = getCompleteDocumentStatus(documents);
                
                // Get full timeline - FIXED: Check if method exists, if not return empty list
                List<Map<String, Object>> timeline = new ArrayList<>();
                try {
                    timeline = applicationDAO.getApplicationTimeline(applicationId);
                } catch (Exception e) {
                    System.err.println("Timeline not available: " + e.getMessage());
                    timeline = new ArrayList<>();
                }
                
                result.put("success", true);
                result.put("application", application);
                result.put("documents", getDocumentDetailsList(documents));
                result.put("documentStatus", documentStatus);
                result.put("timeline", timeline);
                result.put("canEdit", "draft".equals(application.getApplicationStatus()));
                
                // Check if all required documents are uploaded
                boolean allDocumentsUploaded = documentStatus.get("missing") != null && 
                                             documentStatus.get("missing").equals(0);
                result.put("allDocumentsUploaded", allDocumentsUploaded);
                
                // Check if application is complete
                boolean isComplete = isApplicationComplete(application);
                result.put("isComplete", isComplete);
            }
        } catch (NumberFormatException e) {
            result.put("success", false);
            result.put("message", "Invalid application ID");
        }
    }
    
    /**
     * Add comprehensive application data to result
     */
    private void addApplicationData(Application application, int userId, Map<String, Object> result) {
        try {
            // Get documents
            List<Document> documents = documentDAO.getDocumentsByApplicationId(application.getId());
            
            // Get complete document status with all details
            Map<String, Object> documentStatus = getCompleteDocumentStatus(documents);
            result.put("documentStatus", documentStatus);
            result.put("documents", getDocumentDetailsList(documents));
            
            // Get application timeline - FIXED: Safe call with try-catch
            List<Map<String, Object>> timeline = new ArrayList<>();
            try {
                timeline = applicationDAO.getApplicationTimeline(application.getId());
            } catch (Exception e) {
                System.err.println("Timeline not available: " + e.getMessage());
                timeline = new ArrayList<>();
            }
            result.put("timeline", timeline);
            
            // Application status details
            String status = application.getApplicationStatus() != null ? 
                           application.getApplicationStatus() : "draft";
            result.put("canEdit", "draft".equals(status));
            result.put("isSubmitted", "submitted".equals(status) || 
                                      "under_review".equals(status) || 
                                      "interview".equals(status) || 
                                      "approved".equals(status) || 
                                      "declined".equals(status));
            result.put("isUnderReview", "under_review".equals(status));
            result.put("isInterview", "interview".equals(status));
            result.put("isApproved", "approved".equals(status));
            result.put("isDeclined", "declined".equals(status));
            result.put("hasDraft", "draft".equals(status));
            
            // Check if application is complete
            boolean isComplete = isApplicationComplete(application);
            result.put("isApplicationComplete", isComplete);
            
            // Check if all required documents are uploaded
            boolean allDocumentsUploaded = documentStatus.get("missing") != null && 
                                          documentStatus.get("missing").equals(0);
            result.put("allDocumentsUploaded", allDocumentsUploaded);
            
            // Calculate completion percentage
            int completionPercentage = calculateCompletionPercentage(application, documentStatus);
            result.put("completionPercentage", completionPercentage);
            
            // Format dates for display
            Map<String, String> formattedDates = new HashMap<>();
            formattedDates.put("createdAt", formatTimestamp(application.getCreatedAt()));
            formattedDates.put("updatedAt", formatTimestamp(application.getUpdatedAt()));
            formattedDates.put("submissionDate", formatTimestamp(application.getSubmissionDate()));
            formattedDates.put("lastSaved", formatTimestamp(application.getLastSaved()));
            result.put("formattedDates", formattedDates);
            
        } catch (Exception e) {
            e.printStackTrace();
            // Log error but don't fail the whole request
            result.put("documentStatus", getDefaultDocumentStatus());
            result.put("timeline", new ArrayList<>());
            result.put("completionPercentage", 0);
            result.put("allDocumentsUploaded", false);
            result.put("isApplicationComplete", false);
        }
    }
    
    // ... (rest of your existing methods: getUserDataMap, getCompleteDocumentStatus, 
    // getDocumentDetailsList, isApplicationComplete, calculateCompletionPercentage,
    // getDefaultDocumentStatus, isEmpty, formatTimestamp remain the same)
    
    /**
     * Get complete user data as a map with all fields
     */
    private Map<String, Object> getUserDataMap(User user) {
        Map<String, Object> userData = new HashMap<>();
        
        userData.put("id", user.getId());
        userData.put("email", user.getEmail());
        userData.put("firstName", user.getFirstName());
        userData.put("lastName", user.getLastName());
        userData.put("fullName", user.getFullName());
        userData.put("createdAt", formatTimestamp(user.getCreatedAt()));
        userData.put("updatedAt", formatTimestamp(user.getUpdatedAt()));
        
        // Add initials for avatar
        String initials = "";
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            initials += user.getFirstName().charAt(0);
        }
        if (user.getLastName() != null && !user.getLastName().isEmpty()) {
            initials += user.getLastName().charAt(0);
        }
        userData.put("initials", initials.toUpperCase());
        
        return userData;
    }
    
    /**
     * Get complete document status with detailed information
     */
    private Map<String, Object> getCompleteDocumentStatus(List<Document> documents) {
        Map<String, Object> status = new HashMap<>();
        int verified = 0;
        int pending = 0;
        int missing = 0;
        int total = 10; // Total required documents
        
        // Document type mapping with display names and icons
        Map<String, String> docNames = new HashMap<>();
        docNames.put("proof_enrollment", "Proof of Enrollment");
        docNames.put("picture", "2×2 Picture");
        docNames.put("valid_id", "Valid ID");
        docNames.put("report_card", "Report Card");
        docNames.put("birth_cert", "Birth Certificate");
        docNames.put("moral_cert", "Good Moral");
        docNames.put("health_cert", "Health Certificate");
        docNames.put("indigency_cert", "Indigency Certificate");
        docNames.put("income_cert", "Income Certificate");
        docNames.put("reco_letter", "Recommendation Letter");
        
        Map<String, String> docIcons = new HashMap<>();
        docIcons.put("proof_enrollment", "📄");
        docIcons.put("picture", "📷");
        docIcons.put("valid_id", "🆔");
        docIcons.put("report_card", "📊");
        docIcons.put("birth_cert", "👶");
        docIcons.put("moral_cert", "⭐");
        docIcons.put("health_cert", "🏥");
        docIcons.put("indigency_cert", "🏠");
        docIcons.put("income_cert", "💰");
        docIcons.put("reco_letter", "✉️");
        
        // Create map of existing documents
        Map<String, Document> docMap = new HashMap<>();
        for (Document doc : documents) {
            docMap.put(doc.getDocumentType(), doc);
        }
        
        // List to hold detailed document status
        List<Map<String, Object>> documentDetails = new ArrayList<>();
        
        // Check each required document type
        String[] requiredDocs = {
            "proof_enrollment", "picture", "valid_id",
            "report_card", "birth_cert", "moral_cert",
            "health_cert", "indigency_cert", "income_cert",
            "reco_letter"
        };
        
        for (String docType : requiredDocs) {
            Map<String, Object> docInfo = new HashMap<>();
            docInfo.put("type", docType);
            docInfo.put("name", docNames.getOrDefault(docType, docType));
            docInfo.put("icon", docIcons.getOrDefault(docType, "📄"));
            docInfo.put("required", true);
            
            Document doc = docMap.get(docType);
            if (doc == null) {
                docInfo.put("status", "missing");
                docInfo.put("statusText", "Missing");
                docInfo.put("statusColor", "#f44336");
                docInfo.put("statusClass", "document-missing");
                docInfo.put("fileName", null);
                docInfo.put("fileSize", null);
                docInfo.put("uploadedAt", null);
                docInfo.put("uploadedAtFormatted", null);
                docInfo.put("id", null);
                missing++;
            } else {
                String uploadStatus = doc.getUploadStatus() != null ? doc.getUploadStatus() : "pending";
                docInfo.put("status", uploadStatus);
                docInfo.put("fileName", doc.getFileName());
                docInfo.put("fileSize", doc.getFileSize());
                docInfo.put("filePath", doc.getFilePath());
                docInfo.put("uploadedAt", doc.getUploadedAt());
                docInfo.put("uploadedAtFormatted", formatTimestamp(doc.getUploadedAt()));
                docInfo.put("id", doc.getId());
                docInfo.put("mimeType", doc.getMimeType());
                
                if ("verified".equals(uploadStatus)) {
                    docInfo.put("statusText", "Verified");
                    docInfo.put("statusColor", "#4caf50");
                    docInfo.put("statusClass", "document-verified");
                    verified++;
                } else if ("pending".equals(uploadStatus)) {
                    docInfo.put("statusText", "Pending");
                    docInfo.put("statusColor", "#ff9800");
                    docInfo.put("statusClass", "document-pending");
                    pending++;
                } else if ("rejected".equals(uploadStatus)) {
                    docInfo.put("statusText", "Rejected");
                    docInfo.put("statusColor", "#f44336");
                    docInfo.put("statusClass", "document-rejected");
                    docInfo.put("rejectionReason", doc.getRejectionReason());
                    missing++; // Count as missing if rejected
                } else {
                    docInfo.put("statusText", "Missing");
                    docInfo.put("statusColor", "#f44336");
                    docInfo.put("statusClass", "document-missing");
                    missing++;
                }
            }
            documentDetails.add(docInfo);
        }
        
        int completionPercentage = total > 0 ? (verified * 100 / total) : 0;
        
        status.put("verified", verified);
        status.put("pending", pending);
        status.put("missing", missing);
        status.put("total", total);
        status.put("completionPercentage", completionPercentage);
        status.put("documents", documentDetails);
        status.put("allVerified", verified == total);
        status.put("hasMissing", missing > 0);
        status.put("hasPending", pending > 0);
        
        return status;
    }
    
    /**
     * Get document details list for API response
     */
    private List<Map<String, Object>> getDocumentDetailsList(List<Document> documents) {
        List<Map<String, Object>> docList = new ArrayList<>();
        
        for (Document doc : documents) {
            Map<String, Object> docMap = new HashMap<>();
            docMap.put("id", doc.getId());
            docMap.put("applicationId", doc.getApplicationId());
            docMap.put("documentType", doc.getDocumentType());
            docMap.put("fileName", doc.getFileName());
            docMap.put("filePath", doc.getFilePath());
            docMap.put("fileSize", doc.getFileSize());
            docMap.put("mimeType", doc.getMimeType());
            docMap.put("uploadStatus", doc.getUploadStatus());
            docMap.put("uploadedAt", doc.getUploadedAt());
            docMap.put("uploadedAtFormatted", formatTimestamp(doc.getUploadedAt()));
            docMap.put("verifiedAt", doc.getVerifiedAt());
            docMap.put("verifiedAtFormatted", formatTimestamp(doc.getVerifiedAt()));
            docMap.put("rejectionReason", doc.getRejectionReason());
            docList.add(docMap);
        }
        
        return docList;
    }
    
    /**
     * Check if application is complete (all required fields filled)
     */
    private boolean isApplicationComplete(Application app) {
        if (app == null) return false;
        
        // Check required personal information
        if (isEmpty(app.getFirstName()) || isEmpty(app.getLastName()) || 
            isEmpty(app.getSex()) || app.getBirthdate() == null ||
            isEmpty(app.getMobileNumber()) || isEmpty(app.getEmail())) {
            return false;
        }
        
        // Check required address fields
        if (isEmpty(app.getPresentRegion()) || isEmpty(app.getPresentProvince()) ||
            isEmpty(app.getPresentMunicipality()) || isEmpty(app.getPresentBarangay()) ||
            isEmpty(app.getPresentHouseNumber()) || isEmpty(app.getPresentStreet()) ||
            isEmpty(app.getPresentZipCode())) {
            return false;
        }
        
        // Check required academic information
        if (isEmpty(app.getJhsName()) || isEmpty(app.getJhsSchoolId()) || isEmpty(app.getJhsType()) ||
            isEmpty(app.getShsName()) || isEmpty(app.getShsSchoolId()) || isEmpty(app.getShsType()) ||
            isEmpty(app.getTrack()) || isEmpty(app.getStrand()) || app.getGrade12Gwa() == null) {
            return false;
        }
        
        // Check required college choices
        if (isEmpty(app.getCollegeFirst()) || isEmpty(app.getProgramFirst())) {
            return false;
        }
        
        // Check essay
        if (isEmpty(app.getEssay())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Calculate application completion percentage
     */
    private int calculateCompletionPercentage(Application app, Map<String, Object> documentStatus) {
        int totalWeight = 0;
        int completedWeight = 0;
        
        // Personal Information (30%)
        if (!isEmpty(app.getFirstName())) completedWeight += 3;
        if (!isEmpty(app.getLastName())) completedWeight += 3;
        if (!isEmpty(app.getSex())) completedWeight += 3;
        if (app.getBirthdate() != null) completedWeight += 3;
        if (!isEmpty(app.getMobileNumber())) completedWeight += 3;
        if (!isEmpty(app.getEmail())) completedWeight += 3;
        if (!isEmpty(app.getPlaceOfBirth())) completedWeight += 2;
        if (app.getHeight() != null) completedWeight += 2;
        if (app.getWeight() != null) completedWeight += 2;
        if (!isEmpty(app.getFacebookUrl())) completedWeight += 2;
        totalWeight += 26;
        
        // Address Information (15%)
        if (!isEmpty(app.getPresentRegion())) completedWeight += 2;
        if (!isEmpty(app.getPresentProvince())) completedWeight += 2;
        if (!isEmpty(app.getPresentMunicipality())) completedWeight += 2;
        if (!isEmpty(app.getPresentBarangay())) completedWeight += 2;
        if (!isEmpty(app.getPresentHouseNumber())) completedWeight += 2;
        if (!isEmpty(app.getPresentStreet())) completedWeight += 2;
        if (!isEmpty(app.getPresentZipCode())) completedWeight += 2;
        if (!isEmpty(app.getPermanentRegion())) completedWeight += 1;
        if (!isEmpty(app.getPermanentProvince())) completedWeight += 1;
        if (!isEmpty(app.getPermanentMunicipality())) completedWeight += 1;
        if (!isEmpty(app.getPermanentBarangay())) completedWeight += 1;
        if (!isEmpty(app.getPermanentHouseNumber())) completedWeight += 1;
        if (!isEmpty(app.getPermanentStreet())) completedWeight += 1;
        if (!isEmpty(app.getPermanentZipCode())) completedWeight += 1;
        totalWeight += 21;
        
        // Academic Information (25%)
        if (!isEmpty(app.getJhsName())) completedWeight += 3;
        if (!isEmpty(app.getJhsSchoolId())) completedWeight += 2;
        if (!isEmpty(app.getJhsType())) completedWeight += 2;
        if (!isEmpty(app.getShsName())) completedWeight += 3;
        if (!isEmpty(app.getShsSchoolId())) completedWeight += 2;
        if (!isEmpty(app.getShsType())) completedWeight += 2;
        if (!isEmpty(app.getTrack())) completedWeight += 3;
        if (!isEmpty(app.getStrand())) completedWeight += 3;
        if (app.getGrade12Gwa() != null) completedWeight += 3;
        if (!isEmpty(app.getHonorsReceived())) completedWeight += 2;
        totalWeight += 25;
        
        // College Choices (15%)
        if (!isEmpty(app.getCollegeFirst())) completedWeight += 4;
        if (!isEmpty(app.getProgramFirst())) completedWeight += 4;
        if (!isEmpty(app.getCollegeSecond())) completedWeight += 2;
        if (!isEmpty(app.getProgramSecond())) completedWeight += 2;
        if (!isEmpty(app.getCollegeThird())) completedWeight += 2;
        if (!isEmpty(app.getProgramThird())) completedWeight += 1;
        totalWeight += 15;
        
        // Essay (15%)
        if (!isEmpty(app.getEssay())) {
            int wordCount = app.getEssay().trim().split("\\s+").length;
            if (wordCount >= 150) {
                completedWeight += 15;
            } else if (wordCount >= 100) {
                completedWeight += 10;
            } else if (wordCount >= 50) {
                completedWeight += 5;
            }
        }
        totalWeight += 15;
        
        // Documents (10%)
        if (documentStatus != null) {
            int verified = (int) documentStatus.getOrDefault("verified", 0);
            int total = (int) documentStatus.getOrDefault("total", 10);
            completedWeight += (verified * 10 / total); // 10% weight
            totalWeight += 10;
        }
        
        return totalWeight > 0 ? (completedWeight * 100 / totalWeight) : 0;
    }
    
    /**
     * Get default document status when no documents exist
     */
    private Map<String, Object> getDefaultDocumentStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("verified", 0);
        status.put("pending", 0);
        status.put("missing", 10);
        status.put("total", 10);
        status.put("completionPercentage", 0);
        status.put("allVerified", false);
        status.put("hasMissing", true);
        status.put("hasPending", false);
        status.put("documents", new ArrayList<>());
        return status;
    }
    
    /**
     * Check if string is empty or null
     */
    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty() || str.equals("N/A");
    }
    
    /**
     * Format timestamp to readable string
     */
    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return null;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(timestamp);
    }
}