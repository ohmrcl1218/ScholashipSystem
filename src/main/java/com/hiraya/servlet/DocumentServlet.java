package com.hiraya.servlet;

import com.hiraya.dao.DocumentDAO;
import com.hiraya.model.Document;
import com.google.gson.Gson;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/documents/*")
public class DocumentServlet extends HttpServlet {

    private DocumentDAO documentDAO = new DocumentDAO();
    private Gson gson = new Gson();

    // ===========================
    //            GET
    // ===========================
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();

        // Session validation
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("adminId") == null) {
            sendUnauthorized(response);
            return;
        }

        if (pathInfo == null || pathInfo.equals("/")) {
            sendError(response, "Invalid endpoint");
            return;
        }

        String[] parts = pathInfo.split("/");

        try {

            // ===========================
            // View Document
            // GET /api/documents/{docId}/view
            // ===========================
            if (parts.length == 3 && parts[2].equals("view")) {
                int docId = Integer.parseInt(parts[1]);
                serveDocument(docId, response);
                return;
            }

            // ===========================
            // List Documents by Application
            // GET /api/documents/application/{appId}
            // ===========================
            if (parts.length == 3 && parts[1].equals("application")) {
                int appId = Integer.parseInt(parts[2]);

                List<Document> documents =
                        documentDAO.getDocumentsByApplicationId(appId);

                System.out.println("===== DEBUG DOCUMENT JSON =====");
                System.out.println(gson.toJson(documents));
                System.out.println("================================");

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("documents", documents);

                response.setContentType("application/json");
                response.getWriter().print(gson.toJson(result));
                return;

            }

            sendError(response, "Invalid endpoint");

        } catch (NumberFormatException e) {
            sendError(response, "Invalid ID format");
        }
    }

    // ===========================
    //            POST
    // ===========================
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("adminId") == null) {
            sendUnauthorized(response);
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            sendError(response, "Invalid endpoint");
            return;
        }

        String[] parts = pathInfo.split("/");

        try {

            // ===========================
            // Verify Document
            // POST /api/documents/{docId}/verify
            // ===========================
            if (parts.length == 3 && parts[2].equals("verify")) {

                int docId = Integer.parseInt(parts[1]);
                String adminId = session.getAttribute("adminId").toString();

                boolean verified =
                        documentDAO.verifyDocument(docId, adminId);

                Map<String, Object> result = new HashMap<>();
                result.put("success", verified);
                result.put("message",
                        verified ? "Document verified successfully"
                                 : "Failed to verify document");

                out.print(gson.toJson(result));
                return;
            }

            sendError(response, "Invalid endpoint");

        } catch (NumberFormatException e) {
            sendError(response, "Invalid ID format");
        }
    }

    // ===========================
    //     Serve File Content
    // ===========================
    private void serveDocument(int docId,
                               HttpServletResponse response) throws IOException {

        Document document = documentDAO.getDocumentById(docId);

        if (document == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        File file = new File(document.getFilePath());

        if (!file.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(document.getMimeType());
        response.setHeader("Content-Disposition",
                "inline; filename=\"" + document.getFileName() + "\"");
        response.setContentLengthLong(file.length());

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
    }

    // ===========================
    //     Helper Methods
    // ===========================
    private void sendUnauthorized(HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        sendError(response, "Not authenticated");
    }

    private void sendError(HttpServletResponse response,
                           String message) throws IOException {
        response.setContentType("application/json");

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);

        response.getWriter().print(gson.toJson(result));
    }
}
