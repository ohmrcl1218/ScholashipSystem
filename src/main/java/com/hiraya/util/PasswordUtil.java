package com.hiraya.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {
    
    private static final int SALT_LENGTH = 16;
    
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static String hashPassword(String password) {
        String salt = generateSalt();
        return salt + ":" + hashPassword(password, salt);
    }
    
    public static boolean verifyPassword(String password, String storedHash) {
        if (storedHash == null || !storedHash.contains(":")) {
            System.err.println("Invalid stored hash format: " + storedHash);
            return false;
        }
        
        String[] parts = storedHash.split(":");
        if (parts.length != 2) {
            System.err.println("Stored hash has wrong number of parts: " + parts.length);
            return false;
        }
        
        String salt = parts[0];
        String hash = parts[1];
        String computedHash = hashPassword(password, salt);
        
        boolean verified = hash.equals(computedHash);
        System.out.println("Password verification: " + (verified ? "SUCCESS" : "FAILED"));
        
        return verified;
    }
    
    public static String generateUserId(String firstName, String lastName) {
        String prefix = (firstName.length() >= 2 ? firstName.substring(0, 2) : firstName) 
                      + (lastName.length() >= 2 ? lastName.substring(0, 2) : lastName);
        String random = String.format("%04d", (int)(Math.random() * 10000));
        return (prefix + random).toUpperCase();
    }
    
    public static String generateApplicationId(int userId) {
        return "APP-" + String.format("%06d", userId) + "-" + System.currentTimeMillis() % 10000;
    }
    
    public static String generateReferenceNumber() {
        int year = java.time.Year.now().getValue();
        int random = (int)(Math.random() * 90000) + 10000;
        return "HF-" + year + "-" + random;
    }
}