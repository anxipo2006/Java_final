package com.coffeeshop.core.service;

import java.util.Optional;

import com.coffeeshop.core.dao.StaffDAO; // <<<< THÊM IMPORT
import com.coffeeshop.core.dao.UserDAO;
import com.coffeeshop.core.exception.AuthenticationException;
import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.model.User;
import com.coffeeshop.core.util.PasswordUtil;

public class AuthService {

    private UserDAO userDAO;
    private StaffDAO staffDAO; // <<<< THÊM StaffDAO

    public AuthService() {
        this.userDAO = new UserDAO();
        this.staffDAO = new StaffDAO(); // <<<< KHỞI TẠO
    }

    public User login(String username, String password) throws AuthenticationException, DatabaseOperationException {
        // ... (giữ nguyên) ...
        Optional<User> userOpt = userDAO.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (PasswordUtil.checkPassword(password, user.getPasswordHash())) {
                return user;
            }
        }
        throw new AuthenticationException("Tên đăng nhập hoặc mật khẩu không đúng.");
    }

    /**
     * Đăng ký người dùng mới.
     *
     * @param username Tên đăng nhập.
     * @param password Mật khẩu.
     * @param firstName Tên.
     * @param lastName Họ.
     * @param email Email.
     * @param role Vai trò.
     * @param staffIdFk (Tùy chọn) Staff ID để liên kết, chỉ áp dụng nếu role là
     * STAFF.
     * @return User đã được tạo.
     * @throws DatabaseOperationException Nếu username/email đã tồn tại hoặc lỗi
     * CSDL.
     * @throws IllegalArgumentException Nếu thông tin không hợp lệ (vd:
     * staffIdFk bắt buộc nhưng null).
     */
    // Trong AuthService.java -> registerUser(..., String staffIdFk)
    public User registerUser(String username, String password, String firstName, String lastName, String email, User.UserRole role, String staffIdFk)
            throws DatabaseOperationException, IllegalArgumentException {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập và mật khẩu không được để trống.");
        }

        String effectiveStaffIdFk = null;
        // Chỉ kiểm tra và gán staffIdFk nếu nó được cung cấp và role là STAFF
        if (role == User.UserRole.STAFF && staffIdFk != null && !staffIdFk.trim().isEmpty()) {
            if (!staffDAO.findById(staffIdFk.trim()).isPresent()) {
                // Ném lỗi nếu staffIdFk được cung cấp nhưng không hợp lệ
                throw new IllegalArgumentException("Staff ID '" + staffIdFk.trim() + "' được cung cấp không tồn tại.");
            }
            effectiveStaffIdFk = staffIdFk.trim();
        }
        // Nếu role là STAFF nhưng staffIdFk là null (ví dụ user tự đăng ký), thì effectiveStaffIdFk vẫn là null
        // và sẽ được lưu là NULL vào CSDL, admin sẽ liên kết sau.

        User newUser = new User(username, firstName, lastName, email, role, effectiveStaffIdFk);
        return userDAO.createUser(newUser, password);
    }

    // Overload phương thức registerUser cũ hơn (không có staffIdFk) cho trường hợp tự đăng ký mặc định là STAFF
    // hoặc khi Admin tạo User không phải STAFF
    public User registerUser(String username, String password, String firstName, String lastName, String email, User.UserRole role)
            throws DatabaseOperationException, IllegalArgumentException {
        if (role == User.UserRole.STAFF) {
            throw new IllegalArgumentException("Để đăng ký user STAFF, vui lòng cung cấp Staff ID liên kết.");
        }
        return registerUser(username, password, firstName, lastName, email, role, null);
    }
}
