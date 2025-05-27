package com.coffeeshop.core.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    public static String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        if (hashedPassword == null || !hashedPassword.startsWith("$2a$")) {
            System.err.println("Invalid hash provided for password check: " + hashedPassword);
            return false;
        }
        try {
            return BCrypt.checkpw(plainTextPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            System.err.println("Error during password check (likely malformed hash): " + e.getMessage());
            return false;
        }
    }

    // main method for testing (như cũ)
// Trong PasswordUtil.java, thêm hoặc sửa phương thức main như sau:
    public static void main(String[] args) {
        String adminPassword = "admin"; // Mật khẩu bạn muốn dùng cho admin
        String adminHashedPassword = hashPassword(adminPassword);
        System.out.println("Hashed password for 'admin': " + adminHashedPassword);
        // Sao chép chuỗi này để cập nhật vào cơ sở dữ liệu
    }
}
