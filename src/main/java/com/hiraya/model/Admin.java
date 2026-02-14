package com.hiraya.model;

import java.sql.Timestamp;

public class Admin extends User {
    private String adminId;
    private String role; // "Scholarship Administrator" or "Reviewer"
    private String department;
    private String permissions;
    private Timestamp lastLogin;
    
    public Admin() {
        this.setUserType("admin");
    }
    
    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
    
    public Timestamp getLastLogin() { return lastLogin; }
    public void setLastLogin(Timestamp lastLogin) { this.lastLogin = lastLogin; }
    
    // Helper methods
    public boolean isScholarshipAdministrator() {
        return "Scholarship Administrator".equals(role);
    }
    
    public boolean isReviewer() {
        return "Reviewer".equals(role);
    }
    
    public boolean canReviewApplications() {
        return isScholarshipAdministrator() || isReviewer();
    }
    
    public boolean canManageUsers() {
        return isScholarshipAdministrator();
    }
    
    public boolean canManageAdmins() {
        return isScholarshipAdministrator();
    }
    
    public boolean canExportData() {
        return isScholarshipAdministrator();
    }
    
    public boolean canChangeApplicationStatus() {
        return isScholarshipAdministrator() || isReviewer();
    }
    
    public boolean canVerifyDocuments() {
        return isScholarshipAdministrator() || isReviewer();
    }
    
    public String getRoleDisplayName() {
        return role;
    }
}