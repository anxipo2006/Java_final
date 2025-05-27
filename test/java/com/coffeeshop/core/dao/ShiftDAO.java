package com.coffeeshop.core.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.model.Shift;
import com.coffeeshop.core.util.DatabaseConnector;

public class ShiftDAO {

    private Shift mapRowToShift(ResultSet rs) throws SQLException {
        Shift shift = new Shift();
        shift.setShiftId(rs.getString("shift_id"));
        shift.setDayOfWeek(rs.getString("day_of_week"));
        shift.setStartTime(rs.getTime("start_time"));
        shift.setEndTime(rs.getTime("end_time"));
        return shift;
    }

    public Shift createShift(Shift shift) throws DatabaseOperationException {
        if (shift.getShiftId() == null || shift.getShiftId().isEmpty()) {
            throw new DatabaseOperationException("Shift ID không được để trống khi tạo mới.");
        }
        // Các kiểm tra khác có thể thêm ở đây (ví dụ: startTime < endTime)

        String sql = "INSERT INTO shift (shift_id, day_of_week, start_time, end_time) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, shift.getShiftId());
            pstmt.setString(2, shift.getDayOfWeek());
            pstmt.setTime(3, shift.getStartTime());
            pstmt.setTime(4, shift.getEndTime());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseOperationException("Tạo ca làm việc thất bại, không có hàng nào bị ảnh hưởng.");
            }
            return shift;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // Lỗi trùng khóa (shift_id)
                throw new DatabaseOperationException("Tạo ca làm việc thất bại: Shift ID '" + shift.getShiftId() + "' đã tồn tại.", e);
            }
            throw new DatabaseOperationException("Lỗi tạo ca làm việc: " + shift.getShiftId(), e);
        }
    }

    public Optional<Shift> findById(String shiftId) throws DatabaseOperationException {
        String sql = "SELECT * FROM shift WHERE shift_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, shiftId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToShift(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi tìm ca làm việc theo ID: " + shiftId, e);
        }
        return Optional.empty();
    }

    public List<Shift> getAllShifts() throws DatabaseOperationException {
        List<Shift> shifts = new ArrayList<>();
        // Sắp xếp theo ngày trong tuần và thời gian bắt đầu để dễ theo dõi
        String sql = "SELECT * FROM shift ORDER BY "
                + " CASE day_of_week "
                + "   WHEN 'Monday' THEN 1 "
                + "   WHEN 'Tuesday' THEN 2 "
                + "   WHEN 'Wednesday' THEN 3 "
                + "   WHEN 'Thursday' THEN 4 "
                + "   WHEN 'Friday' THEN 5 "
                + "   WHEN 'Saturday' THEN 6 "
                + "   WHEN 'Sunday' THEN 7 "
                + "   ELSE 8 END, start_time";
        try (Connection conn = DatabaseConnector.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                shifts.add(mapRowToShift(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi lấy tất cả ca làm việc", e);
        }
        return shifts;
    }

    public boolean updateShift(Shift shift) throws DatabaseOperationException {
        String sql = "UPDATE shift SET day_of_week = ?, start_time = ?, end_time = ? WHERE shift_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, shift.getDayOfWeek());
            pstmt.setTime(2, shift.getStartTime());
            pstmt.setTime(3, shift.getEndTime());
            pstmt.setString(4, shift.getShiftId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi cập nhật ca làm việc: " + shift.getShiftId(), e);
        }
    }

    public boolean deleteShift(String shiftId) throws DatabaseOperationException {
        // Kiểm tra xem shift này có đang được sử dụng trong rota không
        RotaDAO rotaDAO = new RotaDAO(); // Giả sử đã có RotaDAO
        if (rotaDAO.isShiftInUse(shiftId)) { // Cần tạo phương thức này trong RotaDAO
            throw new DatabaseOperationException("Không thể xóa ca làm việc ID: " + shiftId + ". Ca này đang được sử dụng trong lịch làm việc.");
        }

        String sql = "DELETE FROM shift WHERE shift_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, shiftId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) {
                throw new DatabaseOperationException("Không thể xóa ca làm việc ID: " + shiftId + ". Ca này có liên quan đến dữ liệu khác (ví dụ: rota).", e);
            }
            throw new DatabaseOperationException("Lỗi xóa ca làm việc ID: " + shiftId, e);
        }
    }

    // Main method để test
    public static void main(String[] args) {
        ShiftDAO dao = new ShiftDAO();
        // Tạm thời comment out phần delete vì RotaDAO và isShiftInUse chưa có

        try {
            System.out.println("--- Tất cả Ca Làm Việc ---");
            List<Shift> allShifts = dao.getAllShifts();
            allShifts.forEach(System.out::println);

            String testShiftId = "SH001";
            System.out.println("\n--- Tìm Ca Làm Việc với ID: " + testShiftId + " ---");
            dao.findById(testShiftId).ifPresentOrElse(
                    System.out::println,
                    () -> System.out.println("Không tìm thấy ca làm việc với ID: " + testShiftId)
            );

            System.out.println("\n--- Thêm Ca Làm Việc mới ---");
            // Tạo Time object từ String HH:mm:ss
            Time startTime = Time.valueOf("09:00:00");
            Time endTime = Time.valueOf("17:00:00");
            Shift newShift = new Shift("SH_TEST", "Sunday", startTime, endTime);

            try {
                Shift createdShift = dao.createShift(newShift);
                System.out.println("Thêm thành công: " + createdShift);

                System.out.println("\n--- Cập nhật Ca Làm Việc ---");
                createdShift.setStartTime(Time.valueOf("09:30:00"));
                createdShift.setEndTime(Time.valueOf("17:30:00"));
                if (dao.updateShift(createdShift)) {
                    System.out.println("Cập nhật thành công. Dữ liệu mới:");
                    dao.findById(createdShift.getShiftId()).ifPresent(System.out::println);
                } else {
                    System.out.println("Cập nhật thất bại.");
                }

                System.out.println("\n--- Xóa Ca Làm Việc ---");
                if (dao.deleteShift(createdShift.getShiftId())) { // Sẽ lỗi nếu isShiftInUse chưa có
                    System.out.println("Xóa thành công ca làm việc với ID: " + createdShift.getShiftId());
                } else {
                    System.out.println("Xóa thất bại.");
                }
            } catch (DatabaseOperationException e) {
                System.err.println("Lỗi trong quá trình test CRUD Shift: " + e.getMessage());
            }

        } catch (DatabaseOperationException e) {
            System.err.println("Lỗi DAO Operation: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DatabaseConnector.closeConnection();
        }
    }
}
