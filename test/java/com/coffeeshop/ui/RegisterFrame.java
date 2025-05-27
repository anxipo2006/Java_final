package com.coffeeshop.ui;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints; // <<<< THÊM IMPORT
import java.awt.GridBagLayout;
import java.awt.Insets; // <<<< THÊM IMPORT
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame; // <<<< THÊM IMPORT
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.coffeeshop.core.dao.StaffDAO;
import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.model.Staff;
import com.coffeeshop.core.model.User;
import com.coffeeshop.core.service.AuthService;

public class RegisterFrame extends JFrame {

    // ... (các trường txt... và JPassword... giữ nguyên) ...
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;
    private JTextField txtFirstName;
    private JTextField txtLastName;
    private JTextField txtEmail;
    private JComboBox<User.UserRole> cbRole;
    private JComboBox<StaffWrapper> cbSelectStaff; // <<<< JCOMBOBOX MỚI ĐỂ CHỌN STAFF
    private JLabel lblSelectStaff; // Label cho JComboBox này

    private JButton btnRegister;
    private JButton btnCancel;

    private AuthService authService;
    private StaffDAO staffDAO; // <<<< DAO ĐỂ LẤY DANH SÁCH STAFF
    private LoginFrame loginFrameInstance;

    public RegisterFrame(LoginFrame loginFrame) {
        this.loginFrameInstance = loginFrame;
        this.authService = new AuthService();
        this.staffDAO = new StaffDAO(); // <<<< KHỞI TẠO

        // ... (setTitle, setSize, etc. giữ nguyên) ...
        setTitle("Đăng Ký Tài Khoản Mới");
        setSize(480, 450); // Tăng chiều cao một chút
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(loginFrame);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int y = 0;
        // ... (Thêm Username, Password, Confirm Password, FirstName, LastName, Email như cũ) ...
        gbc.gridx = 0;
        gbc.gridy = y;
        add(new JLabel("Tên đăng nhập (*):"), gbc);
        gbc.gridx = 1;
        gbc.gridy = y++;
        txtUsername = new JTextField(20);
        add(txtUsername, gbc);
        gbc.gridx = 0;
        gbc.gridy = y;
        add(new JLabel("Mật khẩu (*):"), gbc);
        gbc.gridx = 1;
        gbc.gridy = y++;
        txtPassword = new JPasswordField(20);
        add(txtPassword, gbc);
        gbc.gridx = 0;
        gbc.gridy = y;
        add(new JLabel("Xác nhận mật khẩu (*):"), gbc);
        gbc.gridx = 1;
        gbc.gridy = y++;
        txtConfirmPassword = new JPasswordField(20);
        add(txtConfirmPassword, gbc);
        gbc.gridx = 0;
        gbc.gridy = y;
        add(new JLabel("Tên:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = y++;
        txtFirstName = new JTextField(20);
        add(txtFirstName, gbc);
        gbc.gridx = 0;
        gbc.gridy = y;
        add(new JLabel("Họ:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = y++;
        txtLastName = new JTextField(20);
        add(txtLastName, gbc);
        gbc.gridx = 0;
        gbc.gridy = y;
        add(new JLabel("Email (duy nhất):"), gbc);
        gbc.gridx = 1;
        gbc.gridy = y++;
        txtEmail = new JTextField(20);
        add(txtEmail, gbc);

        gbc.gridx = 0;
        gbc.gridy = y;
        add(new JLabel("Vai trò (*):"), gbc);
        gbc.gridx = 1;
        gbc.gridy = y++;
        cbRole = new JComboBox<>(User.UserRole.values());
        add(cbRole, gbc);

        // JComboBox để chọn Staff, chỉ hiển thị nếu role là STAFF và Admin đang tạo
        lblSelectStaff = new JLabel("Liên kết với Nhân viên:");
        gbc.gridx = 0;
        gbc.gridy = y;
        add(lblSelectStaff, gbc);
        cbSelectStaff = new JComboBox<>();
        loadStaffIntoStaffComboBox(); // Load danh sách staff
        gbc.gridx = 1;
        gbc.gridy = y++;
        add(cbSelectStaff, gbc);

        // Logic hiển thị/ẩn cbSelectStaff dựa trên cbRole và người dùng hiện tại
        User currentUser = LoginFrame.getCurrentLoggedInUser();
        boolean isAdminCreating = (currentUser != null && currentUser.getRole() == User.UserRole.ADMIN);

        if (!isAdminCreating) { // Người dùng tự đăng ký hoặc Staff khác tạo (không nên)
            cbRole.setSelectedItem(User.UserRole.STAFF);
            cbRole.setEnabled(false);
            // Nếu người dùng tự đăng ký (chưa ai login), họ không thể liên kết staff_id ngay
            // Staff ID có thể cần được admin gán sau, hoặc quy trình phải là admin tạo staff user
            lblSelectStaff.setVisible(false);
            cbSelectStaff.setVisible(false);
        } else { // Admin đang tạo
            cbRole.setEnabled(true); // Admin được chọn role
            // Listener cho cbRole để ẩn/hiện cbSelectStaff
            cbRole.addActionListener(ae -> {
                boolean staffRoleSelected = (cbRole.getSelectedItem() == User.UserRole.STAFF);
                lblSelectStaff.setVisible(staffRoleSelected);
                cbSelectStaff.setVisible(staffRoleSelected);
            });
            // Kích hoạt listener ban đầu
            boolean staffRoleSelectedInitial = (cbRole.getSelectedItem() == User.UserRole.STAFF);
            lblSelectStaff.setVisible(staffRoleSelectedInitial);
            cbSelectStaff.setVisible(staffRoleSelectedInitial);
        }

        // ... (Button panel và listeners như cũ) ...
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnRegister = new JButton("Đăng Ký");
        btnCancel = new JButton("Hủy");
        buttonPanel.add(btnRegister);
        buttonPanel.add(btnCancel);
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        add(buttonPanel, gbc);
        btnRegister.addActionListener(this::performRegister);
        // ... (listener cho btnCancel và windowClosed như cũ) ...
        btnCancel.addActionListener(e -> closeRegisterFrame());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (loginFrameInstance != null && !loginFrameInstance.isVisible()) {
                    loginFrameInstance.setVisible(true);
                }
            }
        });

    }

    private void closeRegisterFrame() { // ... (giữ nguyên) ...
        this.dispose();
        if (loginFrameInstance != null && !loginFrameInstance.isVisible()) {
            loginFrameInstance.setVisible(true);
        }
    }

    private void loadStaffIntoStaffComboBox() {
        try {
            List<Staff> staffList = staffDAO.getAllStaff();
            cbSelectStaff.removeAllItems();
            cbSelectStaff.addItem(new StaffWrapper(null, "--- Chọn Nhân Viên Liên Kết ---"));
            for (Staff staff : staffList) {
                cbSelectStaff.addItem(new StaffWrapper(staff));
            }
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách nhân viên: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performRegister(ActionEvent e) {
        // ... (lấy username, password, confirmPassword, firstName, lastName, email như cũ) ...
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        String confirmPassword = new String(txtConfirmPassword.getPassword());
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String email = txtEmail.getText().trim();
        User.UserRole role = (User.UserRole) cbRole.getSelectedItem();
        String selectedStaffIdFk = null;

        // ... (các validation username, password, confirmPassword, email như cũ) ...
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            /*...*/ return;
        }
        if (!password.equals(confirmPassword)) {
            /*...*/ return;
        }
        if (password.length() < 6) {
            /*...*/ return;
        }
        if (!email.isEmpty() && !email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            /*...*/ return;
        }

        if (role == User.UserRole.STAFF && cbSelectStaff.isVisible()) {
            StaffWrapper selectedStaffWrapper = (StaffWrapper) cbSelectStaff.getSelectedItem();
            if (selectedStaffWrapper == null || selectedStaffWrapper.getStaff() == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn nhân viên để liên kết cho vai trò STAFF.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            selectedStaffIdFk = selectedStaffWrapper.getStaff().getStaffId();
        }

        try {
            // Gọi authService.registerUser với staffIdFk
            authService.registerUser(username, password, firstName, lastName, email.isEmpty() ? null : email, role, selectedStaffIdFk);
            JOptionPane.showMessageDialog(this, "Đăng ký tài khoản thành công cho: " + username, "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            closeRegisterFrame();

        } catch (DatabaseOperationException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Đăng ký thất bại: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lớp StaffWrapper (tương tự ShiftWrapper)
    private static class StaffWrapper {

        private Staff staff;
        private String displayText;

        public StaffWrapper(Staff staff) {
            this.staff = staff;
            this.displayText = (staff != null) ? (staff.getFullName() + " (ID: " + staff.getStaffId() + ")") : "--- Chọn ---";
        }

        public StaffWrapper(Staff staff, String customText) {
            this.staff = staff;
            this.displayText = customText;
        }

        public Staff getStaff() {
            return staff;
        }

        @Override
        public String toString() {
            return displayText;
        }
    }
}
