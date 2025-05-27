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
import com.coffeeshop.core.model.Staff;
import com.coffeeshop.core.util.DatabaseConnector;

public class StaffDAO {

    private Staff mapRowToStaff(ResultSet rs) throws SQLException {
        Staff staff = new Staff();
        staff.setStaffId(rs.getString("staff_id"));
        staff.setFirstName(rs.getString("first_name"));
        staff.setLastName(rs.getString("last_name"));
        staff.setPosition(rs.getString("position"));
        staff.setSalaryPerHour(rs.getDouble("sal_per_hour"));
        return staff;
    }

    public Staff createStaff(Staff staff) throws DatabaseOperationException {
        if (staff.getStaffId() == null || staff.getStaffId().isEmpty()) {
            throw new DatabaseOperationException("Staff ID không được để trống khi tạo mới.");
        }

        String sql = "INSERT INTO staff (staff_id, first_name, last_name, position, sal_per_hour) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, staff.getStaffId());
            pstmt.setString(2, staff.getFirstName());
            pstmt.setString(3, staff.getLastName());
            pstmt.setString(4, staff.getPosition());
            pstmt.setDouble(5, staff.getSalaryPerHour());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseOperationException("Tạo nhân viên thất bại, không có hàng nào bị ảnh hưởng.");
            }
            return staff;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // Lỗi trùng khóa (staff_id)
                throw new DatabaseOperationException("Tạo nhân viên thất bại: Staff ID '" + staff.getStaffId() + "' đã tồn tại.", e);
            }
            throw new DatabaseOperationException("Lỗi tạo nhân viên: " + staff.getFullName(), e);
        }
    }

    public Optional<Staff> findById(String staffId) throws DatabaseOperationException {
        String sql = "SELECT * FROM staff WHERE staff_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, staffId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToStaff(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi tìm nhân viên theo ID: " + staffId, e);
        }
        return Optional.empty();
    }

    public List<Staff> getAllStaff() throws DatabaseOperationException {
        List<Staff> staffList = new ArrayList<>();
        String sql = "SELECT * FROM staff ORDER BY last_name, first_name";
        try (Connection conn = DatabaseConnector.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                staffList.add(mapRowToStaff(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi lấy tất cả nhân viên", e);
        }
        return staffList;
    }

    public boolean updateStaff(Staff staff) throws DatabaseOperationException {
        String sql = "UPDATE staff SET first_name = ?, last_name = ?, position = ?, sal_per_hour = ? WHERE staff_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, staff.getFirstName());
            pstmt.setString(2, staff.getLastName());
            pstmt.setString(3, staff.getPosition());
            pstmt.setDouble(4, staff.getSalaryPerHour());
            pstmt.setString(5, staff.getStaffId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi cập nhật nhân viên: " + staff.getFullName(), e);
        }
    }

    public boolean deleteStaff(String staffId) throws DatabaseOperationException {
        // Trước khi xóa, kiểm tra xem nhân viên có đang được phân công trong rota không
        // Nếu có, có thể không cho xóa hoặc cần xử lý logic phức tạp hơn (ví dụ: gán lại công việc)
        // Hiện tại DAO chỉ thực hiện xóa. Lớp Service sẽ xử lý logic này.
        RotaDAO rotaDAO = new RotaDAO(); // Giả sử đã có RotaDAO
        if (rotaDAO.hasRotaAssignments(staffId)) { // Cần tạo phương thức này trong RotaDAO
            throw new DatabaseOperationException("Không thể xóa nhân viên ID: " + staffId + ". Nhân viên này đang có lịch làm việc.");
        }

        String sql = "DELETE FROM staff WHERE staff_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, staffId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) { // Lỗi ràng buộc khóa ngoại (ví dụ: nhân viên có trong rota)
                throw new DatabaseOperationException("Không thể xóa nhân viên ID: " + staffId + ". Nhân viên này có liên quan đến dữ liệu khác (ví dụ: rota).", e);
            }
            throw new DatabaseOperationException("Lỗi xóa nhân viên ID: " + staffId, e);
        }
    }

    // Main method để test
    public static void main(String[] args) {
        StaffDAO dao = new StaffDAO();
        // Tạm thời comment out phần delete vì RotaDAO chưa có
        // và hasRotaAssignments chưa được tạo

        try {
            System.out.println("--- Tất cả Nhân viên ---");
            List<Staff> allStaff = dao.getAllStaff();
            allStaff.forEach(s -> System.out.println(s.getFullName() + " (" + s.getStaffId() + ") - " + s.getPosition()));

            String testStaffId = "ST001";
            System.out.println("\n--- Tìm Nhân viên với ID: " + testStaffId + " ---");
            dao.findById(testStaffId).ifPresentOrElse(
                    System.out::println,
                    () -> System.out.println("Không tìm thấy nhân viên với ID: " + testStaffId)
            );

            System.out.println("\n--- Thêm Nhân viên mới ---");
            Staff newStaff = new Staff("ST_TEST", "Test", "User", "Barista", 11.50);
            try {
                Staff createdStaff = dao.createStaff(newStaff);
                System.out.println("Thêm thành công: " + createdStaff);

                System.out.println("\n--- Cập nhật Nhân viên ---");
                createdStaff.setPosition("Senior Barista");
                createdStaff.setSalaryPerHour(12.75);
                if (dao.updateStaff(createdStaff)) {
                    System.out.println("Cập nhật thành công. Dữ liệu mới:");
                    dao.findById(createdStaff.getStaffId()).ifPresent(System.out::println);
                } else {
                    System.out.println("Cập nhật thất bại.");
                }

                System.out.println("\n--- Xóa Nhân viên ---");
                if (dao.deleteStaff(createdStaff.getStaffId())) { // Sẽ lỗi nếu RotaDAO hoặc hasRotaAssignments chưa có
                    System.out.println("Xóa thành công nhân viên với ID: " + createdStaff.getStaffId());
                } else {
                    System.out.println("Xóa thất bại.");
                }
            } catch (DatabaseOperationException e) {
                System.err.println("Lỗi trong quá trình test CRUD Staff: " + e.getMessage());
            }

        } catch (DatabaseOperationException e) {
            System.err.println("Lỗi DAO Operation: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DatabaseConnector.closeConnection();
        }
    }
}
