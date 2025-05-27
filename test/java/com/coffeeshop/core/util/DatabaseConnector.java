package com.coffeeshop.core.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {
    // Thay đổi các thông tin này cho phù hợp với cấu hình MySQL của bạn

    private static final String DB_URL = "jdbc:mysql://localhost:3306/coffeeshop_db"; // Tên CSDL của bạn
    private static final String USER = "root"; // Username MySQL
    private static final String PASS = "Anxl1234@"; // Password MySQL

    private static Connection connection = null;

    // Private constructor để ngăn chặn việc tạo instance từ bên ngoài
    private DatabaseConnector() {
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            try {
                System.out.println("Attempting to establish new database connection..."); // DEBUG
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(DB_URL, USER, PASS);
                System.out.println("Kết nối MySQL thành công qua DatabaseConnector!"); // DEBUG
            } catch (ClassNotFoundException e) {
                System.err.println("Không tìm thấy MySQL JDBC Driver!");
                throw new SQLException("MySQL JDBC Driver not found.", e);
            } catch (SQLException e) {
                System.err.println("Kết nối MySQL thất bại: " + e.getMessage());
                throw e;
            }
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("An error occurred: " + e.getMessage());
            // Log the exception for detailed analysis
            // logger.error("Database connection error", e); // Example using a logger
        }
    }

    // Optional: Test connection
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseConnector.getConnection();
            if (conn != null) {
                System.out.println("Successfully connected to the database!");
                DatabaseConnector.closeConnection();
            } else {
                System.out.println("Failed to make connection!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
