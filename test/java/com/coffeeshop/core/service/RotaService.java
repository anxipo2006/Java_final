package com.coffeeshop.core.service;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional; // Exception mới

import com.coffeeshop.core.dao.RotaDAO; // Cho transaction nếu cần
import com.coffeeshop.core.dao.ShiftDAO;
import com.coffeeshop.core.dao.StaffDAO;
import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.exception.RotaManagementException;
import com.coffeeshop.core.model.RotaEntry;
import com.coffeeshop.core.model.Shift;
import com.coffeeshop.core.model.Staff;
import com.coffeeshop.core.util.DatabaseConnector;

public class RotaService {

    private RotaDAO rotaDAO;
    private ShiftDAO shiftDAO;
    private StaffDAO staffDAO;
    private final int MAX_STAFF_PER_SHIFT = 3; // Giới hạn số nhân viên mỗi ca

    public RotaService() {
        this.rotaDAO = new RotaDAO();
        this.shiftDAO = new ShiftDAO();
        this.staffDAO = new StaffDAO();
    }

    /**
     * Nhân viên đăng ký một ca làm việc vào một ngày cụ thể.
     *
     * @param staffId ID của nhân viên.
     * @param shiftId ID của ca làm việc.
     * @param date Ngày đăng ký.
     * @param rotaId (Tùy chọn) ID của rota chung, có thể tự tạo nếu không cung
     * cấp.
     * @return RotaEntry đã được tạo.
     * @throws RotaManagementException Nếu không thể đăng ký (ca đầy, xung đột
     * lịch, etc.).
     * @throws DatabaseOperationException Nếu có lỗi CSDL.
     */
    public RotaEntry registerForShift(String staffId, String shiftId, Date date, String rotaIdInput)
            throws RotaManagementException, DatabaseOperationException {

        // 1. Kiểm tra sự tồn tại của Staff và Shift
        Optional<Staff> staffOpt = staffDAO.findById(staffId);
        if (!staffOpt.isPresent()) {
            throw new RotaManagementException("Nhân viên với ID " + staffId + " không tồn tại.");
        }
        Optional<Shift> shiftOpt = shiftDAO.findById(shiftId);
        if (!shiftOpt.isPresent()) {
            throw new RotaManagementException("Ca làm việc với ID " + shiftId + " không tồn tại.");
        }
        Shift shift = shiftOpt.get();

        // Sử dụng transaction để đảm bảo tính toàn vẹn
        Connection conn = null;
        try {
            conn = DatabaseConnector.getConnection();
            conn.setAutoCommit(false); // Bắt đầu transaction

            // 2. Kiểm tra số lượng nhân viên đã đăng ký cho ca này vào ngày này
            List<RotaEntry> existingRegistrations = rotaDAO.getRegistrationsForShiftOnDate(shiftId, date, conn); // Cần thêm phương thức này vào RotaDAO
            if (existingRegistrations.size() >= MAX_STAFF_PER_SHIFT) {
                conn.rollback();
                throw new RotaManagementException("Ca làm việc " + shiftId + " vào ngày " + date + " đã đủ " + MAX_STAFF_PER_SHIFT + " nhân viên.");
            }

            // 3. Kiểm tra xem nhân viên này có bị trùng lịch với ca khác không
            // Lấy tất cả các ca nhân viên này đã đăng ký trong ngày 'date'
            List<RotaEntry> staffShiftsOnDate = rotaDAO.getStaffRegistrationsOnDate(staffId, date, conn); // Cần thêm phương thức này vào RotaDAO
            for (RotaEntry existingEntry : staffShiftsOnDate) {
                Optional<Shift> existingShiftOpt = shiftDAO.findById(existingEntry.getShiftId()); // Không cần truyền conn nếu ShiftDAO tự quản lý
                if (existingShiftOpt.isPresent()) {
                    if (doShiftsOverlap(shift, existingShiftOpt.get())) {
                        conn.rollback();
                        throw new RotaManagementException("Nhân viên " + staffId + " đã có lịch làm việc trùng giờ vào ngày " + date + " với ca " + existingEntry.getShiftId());
                    }
                }
            }

            // 4. Nếu tất cả kiểm tra đều qua, tạo RotaEntry mới
            String rotaIdToUse = (rotaIdInput == null || rotaIdInput.trim().isEmpty())
                    ? generateRotaId(date) // Hàm tạo rotaId tự động
                    : rotaIdInput;

            RotaEntry newRotaEntry = new RotaEntry(rotaIdToUse, date, shiftId, staffId);
            // Phương thức createRotaEntry trong RotaDAO bây giờ nên nhận Connection
            RotaEntry createdEntry = rotaDAO.createRotaEntry(newRotaEntry, conn);

            conn.commit(); // Hoàn tất transaction
            return createdEntry;

        } catch (SQLException e) {
            if (conn != null) try {
                conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Lỗi rollback: " + ex.getMessage());
            }
            throw new DatabaseOperationException("Lỗi CSDL khi đăng ký ca: " + e.getMessage(), e);
        } catch (RotaManagementException | DatabaseOperationException e) { // Bắt lại để rollback
            if (conn != null) try {
                conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Lỗi rollback: " + ex.getMessage());
            }
            throw e; // Ném lại
        } finally {
            if (conn != null) try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Lỗi reset autocommit: " + e.getMessage());
            }
            // Không đóng connection ở đây vì nó được quản lý bởi DatabaseConnector
        }
    }

    /**
     * Hủy đăng ký một ca làm việc.
     *
     * @param rowId ID của bản ghi RotaEntry cần hủy.
     * @param staffIdMakingChange ID của nhân viên thực hiện hủy (có thể là
     * chính họ hoặc admin).
     * @throws RotaManagementException Nếu không thể hủy.
     * @throws DatabaseOperationException Nếu có lỗi CSDL.
     */
    public boolean cancelShiftRegistration(int rowId, String staffIdMakingChange)
            throws RotaManagementException, DatabaseOperationException {
        Optional<RotaEntry> entryOpt = rotaDAO.findByRowId(rowId);
        if (!entryOpt.isPresent()) {
            throw new RotaManagementException("Không tìm thấy đăng ký ca với row ID: " + rowId);
        }
        RotaEntry entry = entryOpt.get();

        // Logic kiểm tra quyền hủy (ví dụ: nhân viên chỉ được hủy ca của mình, admin được hủy mọi ca)
        // Hiện tại, giả sử ai cũng có thể hủy nếu biết rowId (cần cải thiện sau)
        // if (!entry.getStaffId().equals(staffIdMakingChange) && !isAdmin(staffIdMakingChange)) {
        //    throw new RotaManagementException("Bạn không có quyền hủy đăng ký này.");
        // }
        return rotaDAO.deleteRotaEntry(rowId);
    }

    // Helper method để kiểm tra xem hai ca có trùng giờ không
    private boolean doShiftsOverlap(Shift shift1, Shift shift2) {
        Time start1 = shift1.getStartTime();
        Time end1 = shift1.getEndTime();
        Time start2 = shift2.getStartTime();
        Time end2 = shift2.getEndTime();

        // Logic kiểm tra trùng lặp: (StartA < EndB) and (EndA > StartB)
        return start1.before(end2) && end1.after(start2);
    }

    // Helper method để tạo rota_id tự động (ví dụ)
    private String generateRotaId(Date date) {
        // Ví dụ: RT-YYYYMMDD
        return "RT-" + new SimpleDateFormat("yyyyMMdd").format(date);
    }

    // Lấy danh sách các ca làm việc còn trống (ít hơn MAX_STAFF_PER_SHIFT) cho một ngày cụ thể
    public List<Shift> getAvailableShiftsForDate(Date date) throws DatabaseOperationException {
        List<Shift> allShifts = shiftDAO.getAllShifts(); // Lấy tất cả các mẫu ca
        List<Shift> availableShifts = new ArrayList<>();

        for (Shift shift : allShifts) {
            // Không cần truyền connection ở đây nếu getRegistrationsForShiftOnDate tự quản lý
            List<RotaEntry> registrations = rotaDAO.getRegistrationsForShiftOnDate(shift.getShiftId(), date);
            if (registrations.size() < MAX_STAFF_PER_SHIFT) {
                availableShifts.add(shift);
            }
        }
        return availableShifts;
    }
}
