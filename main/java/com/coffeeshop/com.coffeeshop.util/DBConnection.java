package com.coffeeshop.com.coffeeshop.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {

    private static final String PROPERTIES_FILE = "db.properties";
    private static String DB_URL;
    private static String USER;
    private static String PASS;

    static {
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            Properties prop = new Properties();
            if (input == null) {
                System.out.println("Sorry, unable to find " + PROPERTIES_FILE);
                // Hoặc ném một RuntimeException ở đây để dừng ứng dụng nếu file config không tìm thấy
            } else {
                prop.load(input);
                DB_URL = prop.getProperty("db.url");
                USER = prop.getProperty("db.user");
                PASS = prop.getProperty("db.password");
            }
            // Đăng ký driver MySQL (không cần thiết cho JDBC 4.0 trở lên nhưng không hại)
            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (Exception e) {
            e.printStackTrace();
            // Ném một RuntimeException để chỉ ra lỗi nghiêm trọng khi khởi tạo kết nối
            throw new RuntimeException("Failed to load database configuration or JDBC driver", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (DB_URL == null || USER == null || PASS == null) {
            throw new SQLException("Database configuration is not loaded properly.");
        }
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    public static void main(String[] args) {
        // Test connection
        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("Connected to the database successfully!");
            } else {
                System.out.println("Failed to make connection!");
            }
        } catch (SQLException e) {
            System.err.println("Connection Failed! Check output console");
            e.printStackTrace();
        }
    }
}
