 // Hoặc package của bạn

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // Thay đổi các thông tin này cho phù hợp với MySQL của bạn
    private static final String DB_URL = "jdbc:mysql://localhost:3306/root"; // Thay your_database_name
    private static final String USER = "coffeesho_db"; // Thay username, ví dụ: root
    private static final String PASS = "Anxl1234@"; // Thay password

    private static Connection connection = null;

    // Private constructor để ngăn việc tạo instance từ bên ngoài (Singleton)
    private DatabaseConnection() {
    }

    public static Connection getConnection() {
        if (connection == null) {
            try {
                // Đăng ký driver (không cần thiết với JDBC 4.0+ nhưng để cho chắc)
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(DB_URL, USER, PASS);
                System.out.println("Kết nối MySQL thành công!");
            } catch (ClassNotFoundException e) {
                System.err.println("Không tìm thấy MySQL JDBC Driver!");
                e.printStackTrace();
            } catch (SQLException e) {
                System.err.println("Kết nối MySQL thất bại!");
                e.printStackTrace();
            }
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null; // Quan trọng: reset để có thể mở lại nếu cần
                System.out.println("Đã đóng kết nối MySQL.");
            } catch (SQLException e) {
                System.err.println("Lỗi khi đóng kết nối MySQL!");
                e.printStackTrace();
            }
        }
    }

    // Phương thức main để test nhanh kết nối
    public static void main(String[] args) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            System.out.println("Test kết nối thành công.");
            DatabaseConnection.closeConnection();
        } else {
            System.out.println("Test kết nối thất bại.");
        }
    }
}
