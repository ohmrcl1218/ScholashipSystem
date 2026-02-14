package com.hiraya.servlet;

import com.hiraya.dao.ApplicationDAO;
import com.hiraya.dao.DocumentDAO;
import com.hiraya.model.Application;
import com.hiraya.model.Document;
import com.hiraya.util.DatabaseUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@WebServlet("/api/application/*")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024, // 1 MB
    maxFileSize = 1024 * 1024 * 10,  // 10 MB
    maxRequestSize = 1024 * 1024 * 50 // 50 MB
)
public class ApplicationServlet extends HttpServlet {
    private ApplicationDAO applicationDAO;
    private DocumentDAO documentDAO;
    private DatabaseUtil dbUtil;
    private Gson gson;
    
    @Override
    public void init() {
        try {
            applicationDAO = new ApplicationDAO();
            documentDAO = new DocumentDAO();
            dbUtil = DatabaseUtil.getInstance();
            
            GsonBuilder builder = new GsonBuilder();
            
            // Register custom deserializer for java.sql.Date
            builder.registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> {
                try {
                    String dateStr = json.getAsString();
                    if (dateStr == null || dateStr.isEmpty()) {
                        return null;
                    }
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                    java.util.Date utilDate = format.parse(dateStr);
                    return new Date(utilDate.getTime());
                } catch (ParseException e) {
                    e.printStackTrace();
                    return null;
                }
            });
            
            gson = builder.create();
            System.out.println("ApplicationServlet initialized successfully");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to initialize ApplicationServlet: " + e.getMessage());
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        HttpSession session = request.getSession(false);
        
        try {
            if (session == null || session.getAttribute("userId") == null) {
                result.put("success", false);
                result.put("message", "Not authenticated");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
            int userId = (int) session.getAttribute("userId");
            String pathInfo = request.getPathInfo();
            
            if (pathInfo == null || pathInfo.equals("/")) {
                // Get user's application
                Application application = applicationDAO.getApplicationByUserId(userId);
                
                if (application != null) {
                    // Get documents
                    List<Document> documents = documentDAO.getDocumentsByApplicationId(application.getId());
                    
                    result.put("success", true);
                    result.put("application", application);
                    result.put("documents", documents);
                    result.put("hasDraft", application.getApplicationStatus().equals("draft"));
                    System.out.println("Retrieved application for user " + userId + ": ID=" + application.getId() + ", Status=" + application.getApplicationStatus());
                } else {
                    result.put("success", false);
                    result.put("message", "No application found");
                    System.out.println("No application found for user " + userId);
                }
            } else if (pathInfo.equals("/has-draft")) {
                // Check if user has draft
                boolean hasDraft = applicationDAO.hasDraft(userId);
                result.put("success", true);
                result.put("hasDraft", hasDraft);
                System.out.println("User " + userId + " has draft: " + hasDraft);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "An error occurred: " + e.getMessage());
        }
        
        response.getWriter().write(gson.toJson(result));
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        HttpSession session = request.getSession(false);
        
        try {
            if (session == null || session.getAttribute("userId") == null) {
                result.put("success", false);
                result.put("message", "Not authenticated");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
            int userId = (int) session.getAttribute("userId");
            String pathInfo = request.getPathInfo();
            
            if (pathInfo != null) {
                switch (pathInfo) {
                    case "/submit":
                        handleSubmit(request, response, userId);
                        break;
                    case "/draft":
                        handleSaveDraft(request, response, userId);
                        break;
                    case "/upload":
                        handleFileUpload(request, response, userId);
                        break;
                    default:
                        result.put("success", false);
                        result.put("message", "Invalid endpoint: " + pathInfo);
                        response.getWriter().write(gson.toJson(result));
                        break;
                }
            } else {
                result.put("success", false);
                result.put("message", "Invalid endpoint");
                response.getWriter().write(gson.toJson(result));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "An error occurred: " + e.getMessage());
            response.getWriter().write(gson.toJson(result));
        }
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        HttpSession session = request.getSession(false);
        
        try {
            if (session == null || session.getAttribute("userId") == null) {
                result.put("success", false);
                result.put("message", "Not authenticated");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
            int userId = (int) session.getAttribute("userId");
            String pathInfo = request.getPathInfo();
            
            if (pathInfo != null && pathInfo.startsWith("/draft/")) {
                try {
                    int applicationId = Integer.parseInt(pathInfo.substring(7));
                    boolean deleted = applicationDAO.deleteDraft(applicationId, userId);
                    
                    if (deleted) {
                        result.put("success", true);
                        result.put("message", "Draft deleted successfully");
                        System.out.println("Draft deleted - User: " + userId + ", Application ID: " + applicationId);
                    } else {
                        result.put("success", false);
                        result.put("message", "Failed to delete draft");
                        System.err.println("Failed to delete draft - User: " + userId + ", Application ID: " + applicationId);
                    }
                } catch (NumberFormatException e) {
                    result.put("success", false);
                    result.put("message", "Invalid application ID format");
                }
            } else {
                result.put("success", false);
                result.put("message", "Invalid endpoint");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "An error occurred: " + e.getMessage());
        }
        
        response.getWriter().write(gson.toJson(result));
    }
    
    private void handleSaveDraft(HttpServletRequest request, HttpServletResponse response, int userId) 
            throws IOException {
        
        Map<String, Object> result = new HashMap<>();
        PrintWriter out = response.getWriter();
        
        try {
            System.out.println("\n===== DRAFT SAVE ATTEMPT =====");
            System.out.println("User ID: " + userId);
            System.out.println("Timestamp: " + new Timestamp(System.currentTimeMillis()));
            
            // Read JSON from request body
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            String jsonData = sb.toString();
            System.out.println("Received JSON: " + jsonData);
            
            if (jsonData.isEmpty()) {
                System.err.println("ERROR: Empty JSON data received");
                result.put("success", false);
                result.put("message", "No data received");
                out.write(gson.toJson(result));
                return;
            }
            
            // Parse JSON to Application object
            Application application = null;
            try {
                application = gson.fromJson(jsonData, Application.class);
                System.out.println("Successfully parsed JSON to Application object");
            } catch (JsonParseException e) {
                System.err.println("ERROR parsing JSON: " + e.getMessage());
                e.printStackTrace();
                result.put("success", false);
                result.put("message", "JSON parsing error: " + e.getMessage());
                out.write(gson.toJson(result));
                return;
            }
            
            if (application == null) {
                System.err.println("ERROR: Application object is null after parsing");
                result.put("success", false);
                result.put("message", "Failed to parse application data");
                out.write(gson.toJson(result));
                return;
            }
            
            // Log all received form data
            System.out.println("\n--- Received Form Data ---");
            System.out.println("First Name: " + application.getFirstName());
            System.out.println("Middle Name: " + application.getMiddleName());
            System.out.println("Last Name: " + application.getLastName());
            System.out.println("Email: " + application.getEmail());
            System.out.println("Mobile: " + application.getMobileNumber());
            System.out.println("Sex: " + application.getSex());
            System.out.println("Birthdate: " + application.getBirthdate());
            System.out.println("Age: " + application.getAge());
            System.out.println("Place of Birth: " + application.getPlaceOfBirth());
            System.out.println("Height: " + application.getHeight());
            System.out.println("Weight: " + application.getWeight());
            System.out.println("Facebook URL: " + application.getFacebookUrl());
            System.out.println("Present Region: " + application.getPresentRegion());
            System.out.println("Present Province: " + application.getPresentProvince());
            System.out.println("Present Municipality: " + application.getPresentMunicipality());
            System.out.println("Present Barangay: " + application.getPresentBarangay());
            System.out.println("JHS Name: " + application.getJhsName());
            System.out.println("SHS Name: " + application.getShsName());
            System.out.println("Track: " + application.getTrack());
            System.out.println("Strand: " + application.getStrand());
            System.out.println("Grade 12 GWA: " + application.getGrade12Gwa());
            System.out.println("College First: " + application.getCollegeFirst());
            System.out.println("Program First: " + application.getProgramFirst());
            System.out.println("Essay length: " + (application.getEssay() != null ? application.getEssay().length() : 0));
            System.out.println("--------------------------\n");
            
            application.setUserId(userId);
            application.setApplicationStatus("draft");
            application.setLastSaved(DatabaseUtil.getCurrentTimestamp());
            
            // Validate required fields
            System.out.println("\n--- Validating Required Fields ---");
            boolean isValid = true;
            StringBuilder missingFields = new StringBuilder();
            
            if (application.getFirstName() == null || application.getFirstName().trim().isEmpty()) {
                isValid = false;
                missingFields.append("First Name, ");
                System.err.println("MISSING: First Name");
            }
            if (application.getLastName() == null || application.getLastName().trim().isEmpty()) {
                isValid = false;
                missingFields.append("Last Name, ");
                System.err.println("MISSING: Last Name");
            }
            if (application.getEmail() == null || application.getEmail().trim().isEmpty()) {
                isValid = false;
                missingFields.append("Email, ");
                System.err.println("MISSING: Email");
            }
            if (application.getMobileNumber() == null || application.getMobileNumber().trim().isEmpty()) {
                isValid = false;
                missingFields.append("Mobile Number, ");
                System.err.println("MISSING: Mobile Number");
            }
            
            if (!isValid) {
                String missing = missingFields.toString();
                if (missing.endsWith(", ")) {
                    missing = missing.substring(0, missing.length() - 2);
                }
                System.err.println("VALIDATION FAILED: Missing fields - " + missing);
                result.put("success", false);
                result.put("message", "Required fields are missing: " + missing);
                out.write(gson.toJson(result));
                return;
            }
            System.out.println("All required fields present");
            
            // Test database connection
            System.out.println("\n--- Testing Database Connection ---");
            if (!testDatabaseConnection()) {
                System.err.println("ERROR: Cannot connect to database!");
                result.put("success", false);
                result.put("message", "Database connection failed");
                out.write(gson.toJson(result));
                return;
            }
            System.out.println("Database connection OK");
            
            // Check if user exists in database
            System.out.println("\n--- Checking User Existence ---");
            if (!userExists(userId)) {
                System.err.println("ERROR: User ID " + userId + " does not exist in database!");
                result.put("success", false);
                result.put("message", "User not found in database");
                out.write(gson.toJson(result));
                return;
            }
            System.out.println("User exists: ID=" + userId);
            
            // Check if user already has a draft
            System.out.println("\n--- Checking Existing Draft ---");
            Application existingDraft = applicationDAO.getApplicationByUserIdAndStatus(userId, "draft");
            if (existingDraft != null) {
                System.out.println("Found existing draft: ID=" + existingDraft.getId() + 
                                 ", Last Saved=" + existingDraft.getLastSaved());
                application.setId(existingDraft.getId());
            } else {
                System.out.println("No existing draft found, will create new");
            }
            
            // Save the draft
            System.out.println("\n--- Saving Draft to Database ---");
            Application saved = null;
            
            try {
                long startTime = System.currentTimeMillis();
                saved = applicationDAO.saveDraft(application);
                long endTime = System.currentTimeMillis();
                System.out.println("Save operation took: " + (endTime - startTime) + "ms");
            } catch (Exception e) {
                System.err.println("EXCEPTION in saveDraft: " + e.getMessage());
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
            
            if (saved != null) {
                System.out.println("\n✓✓✓ DRAFT SAVE SUCCESSFUL! ✓✓✓");
                System.out.println("Application ID: " + saved.getId());
                System.out.println("User ID: " + saved.getUserId());
                System.out.println("Status: " + saved.getApplicationStatus());
                System.out.println("Last Saved: " + saved.getLastSaved());
                System.out.println("Full Name: " + saved.getFirstName() + " " + saved.getLastName());
                System.out.println("Email: " + saved.getEmail());
                System.out.println("================================\n");
                
                result.put("success", true);
                result.put("message", "Draft saved successfully");
                result.put("applicationId", saved.getId());
                result.put("lastSaved", saved.getLastSaved() != null ? saved.getLastSaved().toString() : null);
                
                // Also save to session for immediate use
                HttpSession session = request.getSession();
                session.setAttribute("currentApplicationId", saved.getId());
                
            } else {
                System.err.println("\n✗✗✗ DRAFT SAVE FAILED! ✗✗✗");
                System.err.println("saveDraft returned null - check ApplicationDAO for errors");
                System.err.println("================================\n");
                
                result.put("success", false);
                result.put("message", "Failed to save draft - database operation returned no result");
            }
            
        } catch (Exception e) {
            System.err.println("\n✗✗✗ UNEXPECTED ERROR IN DRAFT SAVE ✗✗✗");
            e.printStackTrace();
            
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            
            result.put("success", false);
            result.put("message", "System error: " + e.getMessage());
            result.put("exception", e.getClass().getName());
            result.put("stacktrace", sw.toString());
        }
        
        System.out.println("Sending response: " + gson.toJson(result));
        System.out.println("===== END DRAFT SAVE ATTEMPT =====\n");
        
        out.write(gson.toJson(result));
    }
    
    private void handleSubmit(HttpServletRequest request, HttpServletResponse response, int userId) 
            throws IOException {
        
        Map<String, Object> result = new HashMap<>();
        PrintWriter out = response.getWriter();
        
        try {
            System.out.println("\n===== APPLICATION SUBMISSION ATTEMPT =====");
            System.out.println("User ID: " + userId);
            
            // Read JSON from request body
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            String jsonData = sb.toString();
            System.out.println("Received JSON: " + jsonData);
            
            Application application = gson.fromJson(jsonData, Application.class);
            application.setUserId(userId);
            
            System.out.println("Application ID to submit: " + application.getId());
            
            // Check if all required documents are uploaded
            System.out.println("Checking required documents...");
            if (!documentDAO.hasAllRequiredDocuments(application.getId())) {
                System.err.println("Missing required documents");
                result.put("success", false);
                result.put("message", "Please upload all required documents before submitting");
                out.write(gson.toJson(result));
                return;
            }
            System.out.println("All required documents present");
            
            Application submitted = applicationDAO.submitApplication(application);
            
            if (submitted != null) {
                System.out.println("\n✓✓✓ APPLICATION SUBMITTED SUCCESSFULLY! ✓✓✓");
                System.out.println("Application ID: " + submitted.getId());
                System.out.println("Reference Number: " + submitted.getReferenceNumber());
                System.out.println("Submission Date: " + submitted.getSubmissionDate());
                System.out.println("========================================\n");
                
                result.put("success", true);
                result.put("message", "Application submitted successfully");
                result.put("applicationId", submitted.getId());
                result.put("referenceNumber", submitted.getReferenceNumber());
                result.put("submissionDate", submitted.getSubmissionDate());
            } else {
                System.err.println("\n✗✗✗ APPLICATION SUBMISSION FAILED! ✗✗✗");
                System.err.println("submitApplication returned null");
                System.err.println("========================================\n");
                
                result.put("success", false);
                result.put("message", "Failed to submit application");
            }
            
        } catch (Exception e) {
            System.err.println("\n✗✗✗ UNEXPECTED ERROR IN SUBMISSION ✗✗✗");
            e.printStackTrace();
            
            result.put("success", false);
            result.put("message", "An error occurred: " + e.getMessage());
        }
        
        System.out.println("Sending response: " + gson.toJson(result));
        System.out.println("===== END SUBMISSION ATTEMPT =====\n");
        
        out.write(gson.toJson(result));
    }
    
    private void handleFileUpload(HttpServletRequest request, HttpServletResponse response, int userId) 
            throws IOException, ServletException {
        
        Map<String, Object> result = new HashMap<>();
        PrintWriter out = response.getWriter();
        
        try {
            System.out.println("\n===== FILE UPLOAD ATTEMPT =====");
            System.out.println("User ID: " + userId);
            
            // Get application ID from request
            String applicationIdParam = request.getParameter("applicationId");
            if (applicationIdParam == null || applicationIdParam.isEmpty()) {
                System.err.println("ERROR: Application ID is required");
                result.put("success", false);
                result.put("message", "Application ID is required");
                out.write(gson.toJson(result));
                return;
            }
            
            int applicationId = Integer.parseInt(applicationIdParam);
            System.out.println("Application ID: " + applicationId);
            
            // Get document type
            String documentType = request.getParameter("documentType");
            if (documentType == null || documentType.isEmpty()) {
                System.err.println("ERROR: Document type is required");
                result.put("success", false);
                result.put("message", "Document type is required");
                out.write(gson.toJson(result));
                return;
            }
            System.out.println("Document Type: " + documentType);
            
            // Get the file part
            Part filePart = request.getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                System.err.println("ERROR: No file uploaded");
                result.put("success", false);
                result.put("message", "No file uploaded");
                out.write(gson.toJson(result));
                return;
            }
            
            String originalFileName = filePart.getSubmittedFileName();
            long fileSize = filePart.getSize();
            System.out.println("File Name: " + originalFileName);
            System.out.println("File Size: " + fileSize + " bytes");
            System.out.println("Content Type: " + filePart.getContentType());
            
            // Get the application
            Application application = applicationDAO.getApplicationById(applicationId);
            if (application == null) {
                System.err.println("ERROR: Application not found with ID: " + applicationId);
                result.put("success", false);
                result.put("message", "Application not found");
                out.write(gson.toJson(result));
                return;
            }
            
            if (application.getUserId() != userId) {
                System.err.println("ERROR: Permission denied - User " + userId + " cannot upload to application " + applicationId);
                result.put("success", false);
                result.put("message", "You don't have permission to upload to this application");
                out.write(gson.toJson(result));
                return;
            }
            
            // Create upload directory if it doesn't exist
            String uploadPath = getServletContext().getRealPath("") + File.separator + "uploads";
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
                System.out.println("Created upload directory: " + uploadPath);
            }
            
            // Generate unique filename
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            
            String fileName = UUID.randomUUID().toString() + fileExtension;
            String filePath = uploadPath + File.separator + fileName;
            
            // Save file
            filePart.write(filePath);
            System.out.println("File saved to: " + filePath);
            
            // Save file info to database
            Document document = new Document();
            document.setApplicationId(applicationId);
            document.setUserId(userId);
            document.setDocumentType(documentType);
            document.setFileName(originalFileName);
            document.setFilePath("uploads/" + fileName);
            document.setFileSize(fileSize);
            document.setMimeType(filePart.getContentType());
            
            Document saved = documentDAO.saveDocument(document);
            
            if (saved != null) {
                System.out.println("\n✓✓✓ FILE UPLOAD SUCCESSFUL! ✓✓✓");
                System.out.println("Document ID: " + saved.getId());
                System.out.println("Application ID: " + saved.getApplicationId());
                System.out.println("Document Type: " + saved.getDocumentType());
                System.out.println("File Name: " + saved.getFileName());
                System.out.println("==============================\n");
                
                result.put("success", true);
                result.put("message", "File uploaded successfully");
                result.put("documentId", saved.getId());
                result.put("fileName", saved.getFileName());
            } else {
                System.err.println("\n✗✗✗ FILE UPLOAD FAILED! ✗✗✗");
                System.err.println("Failed to save document information to database");
                System.err.println("==============================\n");
                
                result.put("success", false);
                result.put("message", "Failed to save document information");
            }
            
        } catch (NumberFormatException e) {
            System.err.println("ERROR: Invalid Application ID format");
            result.put("success", false);
            result.put("message", "Invalid Application ID format");
        } catch (Exception e) {
            System.err.println("\n✗✗✗ UNEXPECTED ERROR IN FILE UPLOAD ✗✗✗");
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "An error occurred: " + e.getMessage());
        }
        
        System.out.println("Sending response: " + gson.toJson(result));
        System.out.println("===== END FILE UPLOAD ATTEMPT =====\n");
        
        out.write(gson.toJson(result));
    }
    
    // Helper methods for debugging
    private boolean userExists(int userId) {
        String sql = "SELECT id FROM users WHERE id = ?";
        try (Connection conn = dbUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            boolean exists = rs.next();
            if (!exists) {
                System.err.println("User ID " + userId + " does not exist in users table");
            }
            return exists;
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error checking if user exists: " + e.getMessage());
            return false;
        }
    }
    
    private boolean testDatabaseConnection() {
        try (Connection conn = dbUtil.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                // Test query to verify connection works
                try (Statement stmt = conn.createStatement()) {
                    ResultSet rs = stmt.executeQuery("SELECT 1");
                    if (rs.next()) {
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Database connection test failed: " + e.getMessage());
            return false;
        }
    }
}