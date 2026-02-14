package com.hiraya.dao;

import com.hiraya.config.DatabaseConfig;
import com.hiraya.model.User;
import com.hiraya.util.PasswordUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    
	public User register(User user, String password) {
	    String sql = "INSERT INTO users (user_id, first_name, last_name, email, phone, password_hash, user_type, status) " +
	                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	    
	    Connection conn = null;
	    PreparedStatement ps = null;
	    ResultSet rs = null;
	    
	    try {
	        conn = DatabaseConfig.getConnection();
	        System.out.println("✅ Database connected for registration");
	        
	        ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	        
	        String userId = PasswordUtil.generateUserId(user.getFirstName(), user.getLastName());
	        String passwordHash = PasswordUtil.hashPassword(password);
	        
	        System.out.println("📝 Registration data:");
	        System.out.println("  - UserID: " + userId);
	        System.out.println("  - Name: " + user.getFirstName() + " " + user.getLastName());
	        System.out.println("  - Email: " + user.getEmail());
	        System.out.println("  - Phone: " + user.getPhone());
	        System.out.println("  - Type: " + user.getUserType());
	        System.out.println("  - Status: " + user.getStatus());
	        
	        ps.setString(1, userId);
	        ps.setString(2, user.getFirstName());
	        ps.setString(3, user.getLastName());
	        ps.setString(4, user.getEmail());
	        ps.setString(5, user.getPhone() != null ? user.getPhone() : "");
	        ps.setString(6, passwordHash);
	        ps.setString(7, user.getUserType() != null ? user.getUserType() : "applicant");
	        ps.setString(8, user.getStatus() != null ? user.getStatus() : "pending");
	        
	        int affectedRows = ps.executeUpdate();
	        System.out.println("✅ Insert affected rows: " + affectedRows);
	        
	        if (affectedRows > 0) {
	            rs = ps.getGeneratedKeys();
	            if (rs.next()) {
	                user.setId(rs.getInt(1));
	                user.setUserId(userId);
	                System.out.println("✅ User registered successfully with ID: " + user.getId());
	                return user;
	            }
	        }
	        
	    } catch (SQLException e) {
	        System.err.println("❌ SQL Error during registration:");
	        System.err.println("  - Error Code: " + e.getErrorCode());
	        System.err.println("  - SQL State: " + e.getSQLState());
	        System.err.println("  - Message: " + e.getMessage());
	        
	        // Specific error handling
	        if (e.getErrorCode() == 1062) {
	            System.err.println("  - Cause: Duplicate entry - email already exists");
	        } else if (e.getErrorCode() == 1364) {
	            System.err.println("  - Cause: Field doesn't have a default value - check if all required fields are provided");
	        } else if (e.getErrorCode() == 1452) {
	            System.err.println("  - Cause: Foreign key constraint fails");
	        } else if (e.getErrorCode() == 1048) {
	            System.err.println("  - Cause: Column cannot be null");
	        }
	        
	        e.printStackTrace();
	    } finally {
	        DatabaseConfig.closeConnection(conn, ps, rs);
	    }
	    
	    System.err.println("❌ Registration failed - returning null");
	    return null;
	}
    
    public User login(String email, String password) {
        String sql = "SELECT * FROM users WHERE email = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (PasswordUtil.verifyPassword(password, storedHash)) {
                    User user = mapResultSetToUser(rs);
                    rs.close();
                    return user;
                }
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public User getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                User user = mapResultSetToUser(rs);
                rs.close();
                return user;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public User getUserByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                User user = mapResultSetToUser(rs);
                rs.close();
                return user;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public boolean updateUser(User user) {
        String sql = "UPDATE users SET first_name = ?, last_name = ?, phone = ?, status = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, user.getFirstName());
            ps.setString(2, user.getLastName());
            ps.setString(3, user.getPhone());
            ps.setString(4, user.getStatus());
            ps.setInt(5, user.getId());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean updatePassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            String passwordHash = PasswordUtil.hashPassword(newPassword);
            ps.setString(1, passwordHash);
            ps.setInt(2, userId);
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
    
    public List<User> getUsersByType(String userType) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE user_type = ? ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, userType);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
    
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUserId(rs.getString("user_id"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setUserType(rs.getString("user_type"));
        user.setStatus(rs.getString("status"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setUpdatedAt(rs.getTimestamp("updated_at"));
        return user;
    }
}