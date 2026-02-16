package com.hiraya.servlet;

import com.hiraya.dao.AdminDAO;
import com.hiraya.model.Admin;
import com.google.gson.Gson;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/admin/dashboard/applications")
public class AdminApplicationServlet extends HttpServlet {

    private AdminDAO adminDAO = new AdminDAO();
    private Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Get admin from session
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\": false, \"message\": \"No session\"}");
            return;
        }

        Admin admin = (Admin) session.getAttribute("admin");
        if (admin == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\": false, \"message\": \"Admin not logged in\"}");
            return;
        }

        // Get query parameters
        String status = request.getParameter("status");
        String search = request.getParameter("search");
        int page = 1;
        int limit = 10;

        try {
            page = Integer.parseInt(request.getParameter("page"));
        } catch (NumberFormatException e) {
            // keep default
        }

        try {
            limit = Integer.parseInt(request.getParameter("limit"));
        } catch (NumberFormatException e) {
            // keep default
        }

        // Fetch applications
        List<Map<String, Object>> applications = adminDAO.getAllApplications(admin, status, search, page, limit);
        
        // Get total count for pagination
        int totalCount = applications.size(); // You'll need a separate method to get total count
        int totalPages = (int) Math.ceil((double) totalCount / limit);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("success", true);
        responseData.put("applications", applications);
        responseData.put("totalPages", totalPages);
        responseData.put("currentPage", page);

        out.print(gson.toJson(responseData));
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Get admin from session
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\": false, \"message\": \"No session\"}");
            return;
        }

        Admin admin = (Admin) session.getAttribute("admin");
        if (admin == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\": false, \"message\": \"Admin not logged in\"}");
            return;
        }

        String action = request.getParameter("action");
        
        if ("updateStatus".equals(action)) {
            // Handle status update
            int appId = Integer.parseInt(request.getParameter("id"));
            String status = request.getParameter("status");
            
            boolean updated = adminDAO.updateApplicationStatus(admin, appId, status, "", admin.getId());
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", updated);
            if (updated) {
                responseData.put("message", "Status updated successfully");
            } else {
                responseData.put("message", "Failed to update status");
            }
            
            out.print(gson.toJson(responseData));
        } else {
            out.print("{\"success\": false, \"message\": \"Invalid action\"}");
        }
        
        out.flush();
    }
}