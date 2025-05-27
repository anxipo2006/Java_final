package com.coffeeshop.core.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.model.User;
import com.coffeeshop.core.util.DatabaseConnector;
import com.coffeeshop.core.util.PasswordUtil;

public class UserDAO {

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setRole(User.UserRole.fromString(rs.getString("role")));
        user.setStaffIdFk(rs.getString("staff_id_fk"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }

    public User createUser(User user, String plainPassword) throws DatabaseOperationException {
        if (findByUsername(user.getUsername()).isPresent()) { // findByUsername phải tồn tại
            throw new DatabaseOperationException("Username '" + user.getUsername() + "' đã tồn tại.");
        }
        if (user.getEmail() != null && !user.getEmail().isEmpty() && findByEmail(user.getEmail()).isPresent()) { // findByEmail phải tồn tại
            throw new DatabaseOperationException("Email '" + user.getEmail() + "' đã được sử dụng.");
        }

        String hashedPassword = PasswordUtil.hashPassword(plainPassword);
        user.setPasswordHash(hashedPassword);

        String sql = "INSERT INTO users (username, password_hash, first_name, last_name, email, role, staff_id_fk) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getFirstName());
            pstmt.setString(4, user.getLastName());
            pstmt.setString(5, user.getEmail());
            pstmt.setString(6, user.getRole().name());
            if (user.getRole() == User.UserRole.STAFF && user.getStaffIdFk() != null) {
                pstmt.setString(7, user.getStaffIdFk());
            } else {
                pstmt.setNull(7, java.sql.Types.VARCHAR);
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseOperationException("Tạo user thất bại.");
            }
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Tạo user thất bại, không lấy được ID.");
                }
            }
            return user;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new DatabaseOperationException("Tạo user thất bại: Username hoặc Email đã tồn tại.", e);
            }
            if (e.getErrorCode() == 1452) {
                throw new DatabaseOperationException("Tạo user thất bại: Staff ID liên kết (" + user.getStaffIdFk() + ") không hợp lệ hoặc không tồn tại.", e);
            }
            throw new DatabaseOperationException("Lỗi khi tạo user: " + e.getMessage(), e);
        }
    }

    // ============== KHÔI PHỤC CÁC PHƯƠNG THỨC BỊ THIẾU TỪ ĐÂY =================
    public Optional<User> findById(int userId) throws DatabaseOperationException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        System.out.println("[UserDAO] Attempting to findById: " + userId); // DEBUG
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (conn == null || conn.isClosed()) {
                System.err.println("[UserDAO] Connection is null or closed in findById for: " + userId);
                throw new DatabaseOperationException("Database connection error in findById.");
            }
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                System.out.println("[UserDAO] User found by ID: " + userId);
                return Optional.of(mapRowToUser(rs));
            } else {
                System.out.println("[UserDAO] User NOT found by ID: " + userId);
                return Optional.empty();
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] SQLException in findById for: " + userId);
            e.printStackTrace();
            throw new DatabaseOperationException("Error finding user by ID: " + userId, e);
        }
    }

    public Optional<User> findByUsername(String username) throws DatabaseOperationException {
        String sql = "SELECT * FROM users WHERE username = ?";
        System.out.println("[UserDAO] Attempting to findByUsername: " + username); // DEBUG

        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (conn == null || conn.isClosed()) {
                System.err.println("[UserDAO] Connection is null or closed in findByUsername for: " + username);
                throw new DatabaseOperationException("Database connection error in findByUsername.");
            }

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("[UserDAO] User found by username: " + username);
                return Optional.of(mapRowToUser(rs));
            } else {
                System.out.println("[UserDAO] User NOT found by username: " + username);
                return Optional.empty();
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] SQLException in findByUsername for: " + username);
            e.printStackTrace();
            throw new DatabaseOperationException("Error finding user by username: " + username, e);
        }
    }

    public Optional<User> findByEmail(String email) throws DatabaseOperationException {
        String sql = "SELECT * FROM users WHERE email = ?";
        System.out.println("[UserDAO] Attempting to findByEmail: " + email); // DEBUG
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (conn == null || conn.isClosed()) {
                System.err.println("[UserDAO] Connection is null or closed in findByEmail for: " + email);
                throw new DatabaseOperationException("Database connection error in findByEmail.");
            }
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                System.out.println("[UserDAO] User found by email: " + email);
                return Optional.of(mapRowToUser(rs));
            } else {
                System.out.println("[UserDAO] User NOT found by email: " + email);
                return Optional.empty();
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] SQLException in findByEmail for: " + email);
            e.printStackTrace();
            throw new DatabaseOperationException("Error finding user by email: " + email, e);
        }
    }

    public List<User> getAllUsers() throws DatabaseOperationException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username";
        try (Connection conn = DatabaseConnector.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Error getting all users", e);
        }
        return users;
    }

    public boolean updateUser(User user) throws DatabaseOperationException {
        String sql = "UPDATE users SET first_name = ?, last_name = ?, email = ?, role = ?, staff_id_fk = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getFirstName());
            pstmt.setString(2, user.getLastName());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getRole().name());
            if (user.getRole() == User.UserRole.STAFF && user.getStaffIdFk() != null) {
                pstmt.setString(5, user.getStaffIdFk());
            } else {
                pstmt.setNull(5, java.sql.Types.VARCHAR);
            }
            pstmt.setInt(6, user.getUserId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 && e.getMessage().toLowerCase().contains("email")) {
                throw new DatabaseOperationException("Cập nhật user thất bại: Email '" + user.getEmail() + "' đã được sử dụng.", e);
            }
            if (e.getErrorCode() == 1452) {
                throw new DatabaseOperationException("Cập nhật user thất bại: Staff ID liên kết (" + user.getStaffIdFk() + ") không hợp lệ hoặc không tồn tại.", e);
            }
            throw new DatabaseOperationException("Lỗi cập nhật user ID: " + user.getUserId(), e);
        }
    }

    public boolean changeUserPassword(int userId, String newPlainPassword) throws DatabaseOperationException {
        String newHashedPassword = PasswordUtil.hashPassword(newPlainPassword);
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newHashedPassword);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi đổi mật khẩu cho user ID: " + userId, e);
        }
    }

    public boolean deleteUser(int userId) throws DatabaseOperationException {
        // Cân nhắc thêm kiểm tra ràng buộc nghiệp vụ ở Service layer trước khi gọi delete
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi xóa user ID: " + userId, e);
        }
    }

    // ===================== KẾT THÚC PHẦN KHÔI PHỤC ========================
    public static void main(String[] args) {
        UserDAO dao = new UserDAO();
        StaffDAO staffDAO = new StaffDAO(); // Để lấy staff_id hợp lệ cho test
        try {
            String testStaffId = "ST_FOR_USER";
            if (!staffDAO.findById(testStaffId).isPresent()) {
                // Giả sử StaffDAO.createStaff không cần transaction và tự quản lý connection
                staffDAO.createStaff(new com.coffeeshop.core.model.Staff(testStaffId, "Linked", "Staffer", "Barista", 10.0));
                System.out.println("Đã tạo staff mẫu: " + testStaffId);
            }

            System.out.println("--- Tạo User STAFF Mới với staff_id_fk ---");
            User newUserStaff = new User("stafflinked", "Staff", "Linked", "stafflinked@example.com", User.UserRole.STAFF, testStaffId);
            User createdUserStaff = null;
            try {
                createdUserStaff = dao.createUser(newUserStaff, "password123");
                System.out.println("Tạo user STAFF thành công: " + createdUserStaff);
            } catch (DatabaseOperationException e) {
                System.err.println("Lỗi tạo user STAFF: " + e.getMessage());
                Optional<User> existing = dao.findByUsername("stafflinked"); // Cần findByUsername ở đây
                if (existing.isPresent()) {
                    createdUserStaff = existing.get();
                }
            }

            if (createdUserStaff != null) {
                System.out.println("User STAFF được tìm thấy/tạo: " + createdUserStaff);
            }

        } catch (DatabaseOperationException e) {
            e.printStackTrace();
        } finally {
            // DatabaseConnector.closeConnection(); // Nên được quản lý bởi shutdown hook
        }
    }
}
