package com.coffeeshop.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.coffeeshop.core.model.User;

public class MainDashboardFrame extends JFrame {

    private final User currentUser;

    public MainDashboardFrame(User loggedInUser) {
        this.currentUser = loggedInUser;

        setTitle("Bảng Điều Khiển Chính - Coffee Shop (" + currentUser.getUsername() + ")");
        setSize(600, 400); // Kích thước có thể điều chỉnh
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Thoát ứng dụng khi đóng dashboard
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Panel chào mừng và thông tin người dùng
        JPanel welcomePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        welcomePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel lblWelcome = new JLabel("Xin chào, " + currentUser.getFullName() + "! Vai trò: " + currentUser.getRole());
        lblWelcome.setFont(new Font("Arial", Font.BOLD, 16));
        welcomePanel.add(lblWelcome);
        add(welcomePanel, BorderLayout.NORTH);

        // Panel chứa các nút chức năng
        JPanel buttonPanel = new JPanel(new GridLayout(0, 2, 20, 20)); // 0 hàng (tự động), 2 cột, khoảng cách 20px
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton btnOpenManagement = createStyledButton("Mở Quản Lý Tổng Hợp");
        btnOpenManagement.addActionListener(this::openManagementTabs);
        buttonPanel.add(btnOpenManagement);

        JButton btnQuickOrder = createStyledButton("Tạo Đơn Hàng Nhanh");
        btnQuickOrder.addActionListener(e -> openQuickOrderInterface());
        // Hiện tại, chưa có QuickOrderInterface riêng, nên có thể mở tab Order trong Management
        // Hoặc disable nút này nếu chưa có
        // buttonPanel.add(btnQuickOrder); // Tạm thời disable nếu chưa có chức năng riêng

        JButton btnViewReports = createStyledButton("Xem Báo Cáo");
        // btnViewReports.addActionListener(e -> openReports()); // Sẽ làm sau
        btnViewReports.setEnabled(false); // Tạm disable
        buttonPanel.add(btnViewReports);

        JButton btnUserProfile = createStyledButton("Hồ Sơ Cá Nhân");
        // btnUserProfile.addActionListener(e -> openUserProfile()); // Sẽ làm sau
        btnUserProfile.setEnabled(false); // Tạm disable
        buttonPanel.add(btnUserProfile);

        // Nút Logout
        JButton btnLogout = createStyledButton("Đăng Xuất");
        btnLogout.setBackground(new Color(220, 53, 69)); // Màu đỏ cho logout
        btnLogout.setForeground(Color.WHITE);
        btnLogout.addActionListener(e -> performLogout());
        // Đặt nút logout ở vị trí khác hoặc panel khác nếu muốn
        // Hoặc có thể để trong một menu

        // Thêm nút logout vào một panel riêng ở dưới cùng
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        southPanel.add(btnLogout);

        add(buttonPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setMargin(new Insets(10, 15, 10, 15));
        button.setPreferredSize(new Dimension(200, 60)); // Kích thước nút
        // Thêm các style khác nếu muốn
        return button;
    }

    private void openManagementTabs(ActionEvent e) {
        // Mở cửa sổ chứa JTabbedPane (tức là MainApp.showMainApplicationWindow())
        main.showMainApplicationWindow(); // Gọi phương thức static đã có
        // Không cần đóng dashboard này ngay, người dùng có thể quay lại
        // Hoặc nếu muốn chỉ có 1 cửa sổ chính, thì có thể dispose() dashboard
        // this.dispose(); // Tùy theo luồng bạn muốn
    }

    private void openQuickOrderInterface() {
        JOptionPane.showMessageDialog(this, "Chức năng Tạo Đơn Hàng Nhanh sẽ được phát triển sau.", "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
        // Tạm thời, có thể mở tab Order trong Management
        // main.showMainApplicationWindow();
        // Sau đó tìm cách focus vào tab Order (nếu MainApp lưu instance của JTabbedPane)
    }

    private void performLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn đăng xuất?", "Xác Nhận Đăng Xuất",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            LoginFrame.logout(); // Reset người dùng hiện tại
            this.dispose();      // Đóng cửa sổ Dashboard
            // main.java sẽ quản lý việc đóng cửa sổ JTabbedPane và hiển thị lại LoginFrame
            main.showLoginWindowAgain(); // Tạo một hàm mới trong main.java để chỉ hiển thị LoginFrame
        }
    }
}
