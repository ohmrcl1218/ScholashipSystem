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
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

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

        GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.registerTypeAdapter(Date.class,
                (JsonSerializer<Date>) (src, type, ctx) ->
                        src == null ? null :
                                new JsonPrimitive(new SimpleDateFormat("yyyy-MM-dd").format(src)));

        gsonBuilder.registerTypeAdapter(Timestamp.class,
                (JsonSerializer<Timestamp>) (src, type, ctx) ->
                        src == null ? null :
                                new JsonPrimitive(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(src)));

        gsonBuilder.setPrettyPrinting();
        gson = gsonBuilder.create();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> result = new HashMap<>();
        HttpSession session = request.getSession(false);

        try {

            if (session == null || session.getAttribute("userId") == null) {
                result.put("authenticated", false);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            int userId = (int) session.getAttribute("userId");
            User user = userDAO.getUserById(userId);

            if (user == null) {
                result.put("authenticated", false);
                response.getWriter().write(gson.toJson(result));
                return;
            }

            Application application = applicationDAO.getApplicationByUserId(userId);

            result.put("authenticated", true);
            result.put("user", user);
            result.put("application", application);

            if (application != null) {
                addApplicationData(application, result);
            } else {
                result.put("documentStatus", getDefaultDocumentStatus());
                result.put("completionPercentage", 0);
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.put("authenticated", false);
        }

        response.getWriter().write(gson.toJson(result));
    }

    private void addApplicationData(Application application, Map<String, Object> result) {

        List<Document> documents =
                documentDAO.getDocumentsByApplicationId(application.getId());

        System.out.println("===== DASHBOARD DEBUG =====");
        System.out.println("Application ID: " + application.getId());
        System.out.println("Documents retrieved: " + documents.size());
        for (Document doc : documents) {
            System.out.println("  - Type: " + doc.getDocumentType() + 
                             " | Status: " + doc.getUploadStatus() + 
                             " | File: " + doc.getFileName());
        }

        Map<String, Object> documentStatus =
                getCompleteDocumentStatus(documents);

        result.put("documentStatus", documentStatus);

        int completion =
                calculateCompletionPercentage(application, documentStatus);

        result.put("completionPercentage", completion);
    }

    /**
     * ===============================
     * FIXED DOCUMENT STATUS LOGIC
     * Now includes formatted documents array
     * ===============================
     */
    private Map<String, Object> getCompleteDocumentStatus(List<Document> documents) {

        Map<String, Object> status = new HashMap<>();

        int verified = 0;
        int uploaded = 0;
        int pending = 0;
        int missing = 0;
        int total = 10;

        // Create a map of uploaded documents by type
        Map<String, Document> docMap = new HashMap<>();
        for (Document d : documents) {
            docMap.put(d.getDocumentType(), d);
        }

        // Define all required documents with display information
        // MUST match database values exactly
        String[][] requiredDocs = {
            {Document.BIRTH_CERT, "Birth Certificate", "📄"},
            {Document.REPORT_CARD, "Grade 11 & 12 Report Card", "📊"},
            {Document.MORAL_CERT, "Certificate of Good Moral", "✅"},
            {Document.INDIGENCY_CERT, "Certificate of Indigency", "📋"},
            {Document.INCOME_CERT, "Income Tax Return", "💰"},
            {Document.VALID_ID, "Voter's ID/Certification", "🗳️"},
            {Document.PICTURE, "2x2 ID Picture", "📸"},
            {Document.PROOF_ENROLLMENT, "Proof of Enrollment", "🎓"},
            {Document.HEALTH_CERT, "Health Certificate", "🏥"},
            {Document.RECOMMENDATION_LETTER, "Recommendation Letter", "📝"}
        };

        // Format documents for frontend display
        List<Map<String, Object>> formattedDocs = new ArrayList<>();

        for (String[] docDef : requiredDocs) {
            String type = docDef[0];
            String name = docDef[1];
            String icon = docDef[2];

            Document doc = docMap.get(type);
            Map<String, Object> formattedDoc = new HashMap<>();
            
            formattedDoc.put("type", type);
            formattedDoc.put("name", name);
            formattedDoc.put("icon", icon);

            if (doc == null) {
                // Document not uploaded
                formattedDoc.put("status", "missing");
                formattedDoc.put("statusText", "Missing");
                formattedDoc.put("fileName", null);
                missing++;
            } else {
                String uploadStatus = doc.getUploadStatus() == null ? "pending" : doc.getUploadStatus();
                
                formattedDoc.put("fileName", doc.getFileName());
                formattedDoc.put("fileSize", doc.getFileSize());
                formattedDoc.put("uploadedAt", doc.getUploadedAt());
                
                switch (uploadStatus) {
                    case "verified":
                        formattedDoc.put("status", "verified");
                        formattedDoc.put("statusText", "Verified");
                        verified++;
                        uploaded++;
                        break;

                    case "uploaded":
                    case "pending":
                        formattedDoc.put("status", "pending");
                        formattedDoc.put("statusText", "Pending");
                        uploaded++;
                        pending++;
                        break;

                    case "rejected":
                        formattedDoc.put("status", "missing");
                        formattedDoc.put("statusText", "Rejected");
                        formattedDoc.put("rejectionReason", doc.getRejectionReason());
                        missing++;
                        break;

                    default:
                        formattedDoc.put("status", "missing");
                        formattedDoc.put("statusText", "Missing");
                        missing++;
                        break;
                }
            }

            formattedDocs.add(formattedDoc);
        }

        int completion = total > 0 ? (uploaded * 100 / total) : 0;

        status.put("verified", verified);
        status.put("uploaded", uploaded);
        status.put("pending", pending);
        status.put("missing", missing);
        status.put("total", total);
        status.put("completionPercentage", completion);
        status.put("allVerified", verified == total);
        status.put("hasMissing", missing > 0);
        status.put("hasPending", pending > 0);
        status.put("documents", formattedDocs); // ✅ THIS IS THE KEY FIX

        System.out.println("===== DOCUMENT STATUS SUMMARY =====");
        System.out.println("Verified: " + verified);
        System.out.println("Pending: " + pending);
        System.out.println("Missing: " + missing);
        System.out.println("Formatted docs created: " + formattedDocs.size());
        System.out.println("===================================");

        return status;
    }

    private int calculateCompletionPercentage(Application app,
                                              Map<String, Object> documentStatus) {

        int baseScore = 80;
        int documentWeight = 20;

        int uploaded = (int) documentStatus.getOrDefault("uploaded", 0);
        int total = (int) documentStatus.getOrDefault("total", 10);

        int docScore = total > 0 ? (uploaded * documentWeight / total) : 0;

        return baseScore + docScore;
    }

    private Map<String, Object> getDefaultDocumentStatus() {

        Map<String, Object> status = new HashMap<>();
        List<Map<String, Object>> formattedDocs = new ArrayList<>();

        // Create default document list with all missing
        String[][] defaultDocs = {
            {Document.BIRTH_CERT, "Birth Certificate", "📄"},
            {Document.REPORT_CARD, "Grade 11 & 12 Report Card", "📊"},
            {Document.MORAL_CERT, "Certificate of Good Moral", "✅"},
            {Document.INDIGENCY_CERT, "Certificate of Indigency", "📋"},
            {Document.INCOME_CERT, "Income Tax Return", "💰"},
            {Document.VALID_ID, "Voter's ID/Certification", "🗳️"},
            {Document.PICTURE, "2x2 ID Picture", "📸"},
            {Document.PROOF_ENROLLMENT, "Proof of Enrollment", "🎓"},
            {Document.HEALTH_CERT, "Health Certificate", "🏥"},
            {Document.RECOMMENDATION_LETTER, "Recommendation Letter", "📝"}
        };

        for (String[] doc : defaultDocs) {
            Map<String, Object> formattedDoc = new HashMap<>();
            formattedDoc.put("type", doc[0]);
            formattedDoc.put("name", doc[1]);
            formattedDoc.put("icon", doc[2]);
            formattedDoc.put("status", "missing");
            formattedDoc.put("statusText", "Missing");
            formattedDoc.put("fileName", null);
            formattedDocs.add(formattedDoc);
        }

        status.put("verified", 0);
        status.put("uploaded", 0);
        status.put("pending", 0);
        status.put("missing", 10);
        status.put("total", 10);
        status.put("completionPercentage", 0);
        status.put("allVerified", false);
        status.put("hasMissing", true);
        status.put("hasPending", false);
        status.put("documents", formattedDocs); // ✅ THIS IS THE KEY FIX

        return status;
    }
}