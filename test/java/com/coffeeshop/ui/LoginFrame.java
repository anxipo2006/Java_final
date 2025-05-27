package com.coffeeshop.ui;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.coffeeshop.core.exception.AuthenticationException;
import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.model.User;
import com.coffeeshop.core.service.AuthService;

public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnRegister;
    private AuthService authService;

    private static User currentLoggedInUser = null;
    private static LoginFrame instance; // Singleton pattern để dễ dàng tham chiếu

    private LoginFrame() { // Constructor private cho Singleton
        authService = new AuthService();

        setTitle("Đăng Nhập - Coffee Shop");
        setSize(400, 220); // Giảm chiều cao một chút
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Thoát toàn bộ khi đóng cửa sổ login
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Tên đăng nhập:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        txtUsername = new JTextField(20);
        add(txtUsername, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("Mật khẩu:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        txtPassword = new JPasswordField(20);
        add(txtPassword, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnLogin = new JButton("Đăng Nhập");
        btnRegister = new JButton("Đăng Ký");
        buttonPanel.add(btnLogin);
        buttonPanel.add(btnRegister);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(buttonPanel, gbc);

        btnLogin.addActionListener(this::handleLoginAction);
        btnRegister.addActionListener(e -> openRegisterFrame());

        getRootPane().setDefaultButton(btnLogin);

        // Đảm bảo đóng connection khi cửa sổ này bị đóng (nếu đây là cửa sổ cuối cùng)
        // Tuy nhiên, shutdown hook trong main.java sẽ lo việc này hiệu quả hơn
        // addWindowListener(new WindowAdapter() {
        //     @Override
        //     public void windowClosing(WindowEvent e) {
        //         DatabaseConnector.closeConnection();
        //     }
        // });
    }

    public static LoginFrame getInstance() {
        if (instance == null || !instance.isDisplayable()) {
            instance = new LoginFrame();
        }
        return instance;
    }

    private void handleLoginAction(ActionEvent e) { // Đổi tên
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên đăng nhập và mật khẩu.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            User loggedInUser = authService.login(username, password);
            LoginFrame.currentLoggedInUser = loggedInUser; // Lưu người dùng hiện tại
            JOptionPane.showMessageDialog(this, "Đăng nhập thành công! Xin chào " + loggedInUser.getFullName(), "Thành Công", JOptionPane.INFORMATION_MESSAGE);

            this.dispose(); // Đóng cửa sổ Login

            // Mở MainDashboardFrame thay vì ManagementFrame trực tiếp
            MainDashboardFrame dashboardFrame = new MainDashboardFrame(loggedInUser);
            dashboardFrame.setVisible(true);

        } catch (AuthenticationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Đăng Nhập Thất Bại", JOptionPane.ERROR_MESSAGE);
        } catch (DatabaseOperationException ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối CSDL hoặc thao tác dữ liệu: " + ex.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public void clearFieldsAndFocus() {
        txtUsername.setText("");
        txtPassword.setText("");
        txtUsername.requestFocusInWindow();
    }

    private void openRegisterFrame() {
        RegisterFrame registerFrame = new RegisterFrame(this);
        registerFrame.setVisible(true);
        // Không ẩn LoginFrame ngay, để người dùng có thể quay lại nếu hủy đăng ký
        // this.setVisible(false); 
    }

    public static User getCurrentLoggedInUser() {
        return currentLoggedInUser;
    }

    public static void logout() {
        currentLoggedInUser = null;
        // MainApp sẽ đóng cửa sổ chính và LoginFrame sẽ được hiển thị lại
        // bởi main() trong MainApp.java
    }

    // Hàm này dùng để test riêng LoginFrame, không cần thiết khi chạy từ main.java (MainApp)
    // Nếu muốn giữ lại để test, đảm bảo database có user admin/admin và staff/staff
    // public static void main(String[] args) {
    //     SwingUtilities.invokeLater(() -> {
    //         LoginFrame.getInstance().setVisible(true);
    //     });
    // }
}
