package com.coffeeshop.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.coffeeshop.core.dao.UserDAO;
import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.model.User;

public class SimpleUserManagementUI extends JPanel {

    private UserDAO userDAO;
    private JTable usersTable;
    private DefaultTableModel tableModel;
    // ... (các nút khác nếu có)

    public SimpleUserManagementUI() {
        userDAO = new UserDAO();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createTitledBorder("Quản Lý Người Dùng"));

        // <<<< THÊM CỘT "Staff ID Liên Kết"
        tableModel = new DefaultTableModel(new Object[]{"User ID", "Username", "Họ Tên", "Email", "Vai Trò", "Staff ID Liên Kết", "Ngày Tạo"}, 0);
        usersTable = new JTable(tableModel);
        add(new JScrollPane(usersTable), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefreshUsers = new JButton("Làm Mới Danh Sách");
        btnRefreshUsers.addActionListener(e -> loadUsersData());
        bottomPanel.add(btnRefreshUsers);

        JButton btnAdminCreateUser = new JButton("Tạo User Mới"); // Admin sẽ dùng RegisterFrame
        btnAdminCreateUser.addActionListener(e -> {
            User currentUser = LoginFrame.getCurrentLoggedInUser();
            if (currentUser != null && currentUser.getRole() == User.UserRole.ADMIN) {
                // Khi admin mở RegisterFrame, RegisterFrame sẽ biết và cho phép chọn Role/Staff
                RegisterFrame rf = new RegisterFrame(null);
                rf.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Chức năng này chỉ dành cho Admin.", "Hạn Chế", JOptionPane.WARNING_MESSAGE);
            }
        });
        bottomPanel.add(btnAdminCreateUser);
        // Thêm các nút: Sửa User (đổi role, liên kết staff), Xóa User, Reset Password...
        // Ví dụ:
        // JButton btnEditUser = new JButton("Sửa User");
        // bottomPanel.add(btnEditUser);
        // JButton btnDeleteUser = new JButton("Xóa User");
        // bottomPanel.add(btnDeleteUser);

        add(bottomPanel, BorderLayout.SOUTH);
        loadUsersData();
    }

    private void loadUsersData() {
        tableModel.setRowCount(0);
        try {
            List<User> users = userDAO.getAllUsers();
            for (User user : users) {
                tableModel.addRow(new Object[]{
                    user.getUserId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getRole().name(),
                    user.getStaffIdFk() != null ? user.getStaffIdFk() : "N/A", // <<<< HIỂN THỊ staffIdFk
                    user.getCreatedAt() != null ? user.getCreatedAt().toString().substring(0, 19) : "N/A"
                });
            }
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách người dùng: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Quản Lý Người Dùng");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.add(new SimpleUserManagementUI());
        frame.setVisible(true);
    }

}
