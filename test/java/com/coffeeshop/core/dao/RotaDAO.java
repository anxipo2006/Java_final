package com.coffeeshop.core.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.model.RotaEntry;
import com.coffeeshop.core.util.DatabaseConnector;

public class RotaDAO {

    // DTO RotaEntryView (giữ nguyên như bạn đã cung cấp)
    public static class RotaEntryView {

        public int rowId;
        public String rotaId;
        public String dateFormatted;
        public String shiftId;
        public String shiftInfo;
        public String staffId;
        public String staffName;

        public RotaEntryView(int rowId, String rotaId, Date date, String shiftId, String shiftInfo, String staffId, String staffName) {
            this.rowId = rowId;
            this.rotaId = rotaId;
            this.dateFormatted = new SimpleDateFormat("yyyy-MM-dd (E)").format(date);
            this.shiftId = shiftId;
            this.shiftInfo = shiftInfo;
            this.staffId = staffId;
            this.staffName = staffName;
        }
    }

    private RotaEntry mapRowToRotaEntry(ResultSet rs) throws SQLException {
        RotaEntry entry = new RotaEntry();
        entry.setRowId(rs.getInt("row_id"));
        entry.setRotaId(rs.getString("rota_id"));
        entry.setDate(rs.getDate("date"));
        entry.setShiftId(rs.getString("shift_id"));
        entry.setStaffId(rs.getString("staff_id"));
        return entry;
    }

    private RotaEntryView mapRowToRotaEntryView(ResultSet rs) throws SQLException {
        String shiftDetails = String.format("%s (%s - %s)",
                rs.getString("day_of_week"),
                rs.getTime("start_time").toString().substring(0, 5),
                rs.getTime("end_time").toString().substring(0, 5)
        );
        String staffFullName = rs.getString("first_name") + " " + rs.getString("last_name");

        return new RotaEntryView(
                rs.getInt("r_row_id"),
                rs.getString("rota_id"),
                rs.getDate("r_date"),
                rs.getString("s_shift_id"),
                shiftDetails,
                rs.getString("st_staff_id"),
                staffFullName
        );
    }

    /**
     * Tạo một RotaEntry mới sử dụng Connection được cung cấp (cho transaction).
     */
    public RotaEntry createRotaEntry(RotaEntry rotaEntry, Connection conn) throws DatabaseOperationException {
        // Kiểm tra sự tồn tại của shift_id và staff_id đã được thực hiện ở Service
        String sql = "INSERT INTO rota (rota_id, date, shift_id, staff_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, rotaEntry.getRotaId());
            pstmt.setDate(2, rotaEntry.getDate());
            pstmt.setString(3, rotaEntry.getShiftId());
            pstmt.setString(4, rotaEntry.getStaffId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseOperationException("Tạo rota entry thất bại (không có hàng nào được thêm).");
            }
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    rotaEntry.setRowId(generatedKeys.getInt(1));
                } else {
                    // Nếu CSDL không trả về generated keys hoặc row_id không phải auto_increment,
                    // bạn có thể không lấy được row_id ở đây.
                    // System.out.println("Không lấy được row_id tự tăng cho rota entry mới (nếu không tự tăng thì bình thường).");
                }
            }
            return rotaEntry;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // UNIQUE constraint violation
                throw new DatabaseOperationException("Rota entry này (ngày, ca, nhân viên) có thể đã tồn tại hoặc rota_id bị trùng.", e);
            }
            throw new DatabaseOperationException("Lỗi khi tạo rota entry trong transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Tạo một RotaEntry mới (DAO tự quản lý connection).
     */
    public RotaEntry createRotaEntry(RotaEntry rotaEntry) throws DatabaseOperationException {
        // Kiểm tra sự tồn tại của shift_id và staff_id
        ShiftDAO shiftDAO = new ShiftDAO(); // DAO này sẽ tự lấy connection
        if (!shiftDAO.findById(rotaEntry.getShiftId()).isPresent()) {
            throw new DatabaseOperationException("Không thể tạo rota: Shift ID '" + rotaEntry.getShiftId() + "' không tồn tại.");
        }
        StaffDAO staffDAO = new StaffDAO(); // DAO này sẽ tự lấy connection
        if (!staffDAO.findById(rotaEntry.getStaffId()).isPresent()) {
            throw new DatabaseOperationException("Không thể tạo rota: Staff ID '" + rotaEntry.getStaffId() + "' không tồn tại.");
        }

        Connection conn = null;
        try {
            conn = DatabaseConnector.getConnection();
            // Đặt autoCommit thành true vì DAO này tự quản lý, không phải là một phần của transaction lớn hơn do Service quản lý
            // Tuy nhiên, nếu createRotaEntry(rotaEntry, conn) không thay đổi autoCommit, thì không cần dòng này.
            // conn.setAutoCommit(true); // Đảm bảo auto-commit nếu connection được dùng lại
            return createRotaEntry(rotaEntry, conn);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi SQL khi chuẩn bị tạo Rota Entry (non-tx): " + e.getMessage(), e);
        }
        // Connection sẽ được DatabaseConnector quản lý hoặc đóng bởi ShutdownHook
    }

    /**
     * Lấy các đăng ký cho một ca cụ thể vào một ngày cụ thể, sử dụng Connection
     * được cung cấp.
     */
    public List<RotaEntry> getRegistrationsForShiftOnDate(String shiftId, Date date, Connection conn) throws DatabaseOperationException {
        List<RotaEntry> entries = new ArrayList<>();
        String sql = "SELECT * FROM rota WHERE shift_id = ? AND date = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, shiftId);
            pstmt.setDate(2, date);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                entries.add(mapRowToRotaEntry(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi lấy đăng ký cho ca " + shiftId + " vào ngày " + date + " (trong transaction)", e);
        }
        return entries;
    }

    /**
     * Lấy các đăng ký cho một ca cụ thể vào một ngày cụ thể (DAO tự quản lý
     * connection).
     */
    public List<RotaEntry> getRegistrationsForShiftOnDate(String shiftId, Date date) throws DatabaseOperationException {
        try (Connection conn = DatabaseConnector.getConnection()) {
            return getRegistrationsForShiftOnDate(shiftId, date, conn);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi SQL khi lấy đăng ký ca (non-tx): " + e.getMessage(), e);
        }
    }

    /**
     * Lấy các đăng ký của một nhân viên vào một ngày cụ thể, sử dụng Connection
     * được cung cấp.
     */
    public List<RotaEntry> getStaffRegistrationsOnDate(String staffId, Date date, Connection conn) throws DatabaseOperationException {
        List<RotaEntry> entries = new ArrayList<>();
        String sql = "SELECT * FROM rota WHERE staff_id = ? AND date = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, staffId);
            pstmt.setDate(2, date);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                entries.add(mapRowToRotaEntry(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi lấy đăng ký của nhân viên " + staffId + " vào ngày " + date + " (trong transaction)", e);
        }
        return entries;
    }

    /**
     * Lấy các đăng ký của một nhân viên vào một ngày cụ thể (DAO tự quản lý
     * connection).
     */
    public List<RotaEntry> getStaffRegistrationsOnDate(String staffId, Date date) throws DatabaseOperationException {
        try (Connection conn = DatabaseConnector.getConnection()) {
            return getStaffRegistrationsOnDate(staffId, date, conn);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi SQL khi lấy đăng ký nhân viên (non-tx): " + e.getMessage(), e);
        }
    }

    public Optional<RotaEntry> findByRowId(int rowId) throws DatabaseOperationException {
        // ... (Giữ nguyên, vì đây là thao tác đọc đơn giản, có thể tự quản lý connection) ...
        String sql = "SELECT * FROM rota WHERE row_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, rowId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToRotaEntry(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi tìm rota entry theo row_id: " + rowId, e);
        }
        return Optional.empty();
    }

    public List<RotaEntryView> getAllRotaViews() throws DatabaseOperationException {
        // ... (Giữ nguyên, thao tác đọc) ...
        List<RotaEntryView> views = new ArrayList<>();
        String sql = "SELECT r.row_id AS r_row_id, r.rota_id, r.date AS r_date, "
                + "s.shift_id AS s_shift_id, s.day_of_week, s.start_time, s.end_time, "
                + "st.staff_id AS st_staff_id, st.first_name, st.last_name "
                + "FROM rota r "
                + "JOIN shift s ON r.shift_id = s.shift_id "
                + "JOIN staff st ON r.staff_id = st.staff_id "
                + "ORDER BY r.date DESC, s.start_time, st.last_name";
        try (Connection conn = DatabaseConnector.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                views.add(mapRowToRotaEntryView(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi lấy tất cả rota views: " + e.getMessage(), e);
        }
        return views;
    }

    public List<RotaEntryView> getRotaViewsByDate(Date date) throws DatabaseOperationException {
        // ... (Giữ nguyên, thao tác đọc) ...
        List<RotaEntryView> views = new ArrayList<>();
        String sql = "SELECT r.row_id AS r_row_id, r.rota_id, r.date AS r_date, "
                + "s.shift_id AS s_shift_id, s.day_of_week, s.start_time, s.end_time, "
                + "st.staff_id AS st_staff_id, st.first_name, st.last_name "
                + "FROM rota r "
                + "JOIN shift s ON r.shift_id = s.shift_id "
                + "JOIN staff st ON r.staff_id = st.staff_id "
                + "WHERE r.date = ? "
                + "ORDER BY s.start_time, st.last_name";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, date);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                views.add(mapRowToRotaEntryView(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi lấy rota views theo ngày " + date + ": " + e.getMessage(), e);
        }
        return views;
    }

    public boolean updateRotaEntry(RotaEntry rotaEntry) throws DatabaseOperationException {
        // ... (Giữ nguyên, hoặc thêm phiên bản nhận Connection nếu cần update trong transaction) ...
        String sql = "UPDATE rota SET rota_id = ?, date = ?, shift_id = ?, staff_id = ? WHERE row_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, rotaEntry.getRotaId());
            pstmt.setDate(2, rotaEntry.getDate());
            pstmt.setString(3, rotaEntry.getShiftId());
            pstmt.setString(4, rotaEntry.getStaffId());
            pstmt.setInt(5, rotaEntry.getRowId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi cập nhật rota entry row_id: " + rotaEntry.getRowId(), e);
        }
    }

    // Phương thức deleteRotaEntry nên nhận Connection nếu được gọi từ Service trong transaction
    public boolean deleteRotaEntry(int rowId, Connection conn) throws DatabaseOperationException {
        String sql = "DELETE FROM rota WHERE row_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, rowId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi xóa rota entry row_id: " + rowId + " (trong transaction)", e);
        }
    }

    // Phiên bản delete tự quản lý connection
    public boolean deleteRotaEntry(int rowId) throws DatabaseOperationException {
        try (Connection conn = DatabaseConnector.getConnection()) {
            // Nếu bạn muốn transaction cho cả delete thì phải truyền conn từ Service
            // Hoặc Service tự quản lý delete. Hiện tại, delete này là auto-commit.
            return deleteRotaEntry(rowId, conn);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi SQL khi xóa Rota Entry (non-tx): " + e.getMessage(), e);
        }
    }

    public boolean hasRotaAssignments(String staffId) throws DatabaseOperationException {
        // ... (Giữ nguyên) ...
        String sql = "SELECT COUNT(*) FROM rota WHERE staff_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, staffId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi kiểm tra rota assignment cho staff_id: " + staffId, e);
        }
        return false;
    }

    public boolean isShiftInUse(String shiftId) throws DatabaseOperationException {
        // ... (Giữ nguyên) ...
        String sql = "SELECT COUNT(*) FROM rota WHERE shift_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, shiftId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi kiểm tra shift in use cho shift_id: " + shiftId, e);
        }
        return false;
    }

    public static void main(String[] args) { // ... (Giữ nguyên main test) ...
        RotaDAO dao = new RotaDAO();
        ShiftDAO shiftDAO = new ShiftDAO();
        StaffDAO staffDAO = new StaffDAO();

        try {
            System.out.println("--- Tất cả Rota Entries (View) ---");
            List<RotaEntryView> allRotaViews = dao.getAllRotaViews();
            allRotaViews.subList(0, Math.min(allRotaViews.size(), 10)).forEach(rv
                    -> System.out.println(String.format(
                            "RowID: %d, RotaID: %s, Date: %s, Shift: %s (ID: %s), Staff: %s (ID: %s)",
                            rv.rowId, rv.rotaId, rv.dateFormatted, rv.shiftInfo, rv.shiftId, rv.staffName, rv.staffId
                    ))
            );

            System.out.println("\n--- Rota Entries cho ngày 2024-02-12 ---");
            Date testDate = Date.valueOf("2024-02-12");
            List<RotaEntryView> rotaByDate = dao.getRotaViewsByDate(testDate);
            rotaByDate.forEach(rv -> System.out.println(String.format(
                    "  -> RotaID: %s, Shift: %s, Staff: %s",
                    rv.rotaId, rv.shiftInfo, rv.staffName
            )));

            System.out.println("\n--- Test Tạo Rota Entry mới ---");
            String testStaffId = "ST004";
            String testShiftId = "SH001";
            Date newRotaDate = Date.valueOf("2024-02-20"); // Đổi ngày để tránh trùng
            String newRotaId = "RT_TEST_" + System.currentTimeMillis() % 1000; // Đảm bảo ID mới

            if (staffDAO.findById(testStaffId).isPresent() && shiftDAO.findById(testShiftId).isPresent()) {
                RotaEntry newEntry = new RotaEntry(newRotaId, newRotaDate, testShiftId, testStaffId);
                try {
                    // Sử dụng phiên bản createRotaEntry tự quản lý connection cho test này
                    RotaEntry createdEntry = dao.createRotaEntry(newEntry);
                    System.out.println("Tạo rota entry thành công: " + createdEntry);

                    createdEntry.setRotaId(newRotaId + "_UPDATED");
                    if (dao.updateRotaEntry(createdEntry)) {
                        System.out.println("Update rota_id thành công cho row_id: " + createdEntry.getRowId());
                    }

                    System.out.println("--- Test Xóa Rota Entry ---");
                    // Sử dụng phiên bản deleteRotaEntry tự quản lý connection cho test này
                    if (dao.deleteRotaEntry(createdEntry.getRowId())) {
                        System.out.println("Xóa rota entry thành công, row_id: " + createdEntry.getRowId());
                    }
                } catch (DatabaseOperationException e) {
                    System.err.println("Lỗi khi test CRUD RotaEntry: " + e.getMessage());
                }
            } else {
                System.out.println("Không thể test, staff " + testStaffId + " hoặc shift " + testShiftId + " không tồn tại.");
            }

        } catch (DatabaseOperationException e) {
            System.err.println("Lỗi DAO Operation: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // DatabaseConnector.closeConnection(); // Không đóng ở đây nếu DatabaseConnector là singleton
        }
    }
}
