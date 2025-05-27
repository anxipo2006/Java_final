package com.coffeeshop.ui;

import java.sql.Connection;
import java.sql.SQLException;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.coffeeshop.core.dao.UserDAO;
import com.coffeeshop.core.exception.DatabaseOperationException; // Import StaffDAO
import com.coffeeshop.core.model.User;
import com.coffeeshop.core.service.AuthService; // Import Staff model
import com.coffeeshop.core.util.DatabaseConnector;

public class main {

    private static JFrame managementFrameInstance;
    // Không cần loginFrameInstance là static ở đây nữa nếu LoginFrame tự quản lý instance
    // private static LoginFrame loginFrameInstance; 

    public static void showMainApplicationWindow() {
        User currentUser = LoginFrame.getCurrentLoggedInUser();
        if (currentUser == null) {
            System.err.println("Lỗi: Người dùng chưa đăng nhập. Hiển thị lại cửa sổ Login.");
            showLoginWindowAgain(); // Gọi hàm này để đảm bảo cửa sổ login được hiển thị
            return;
        }

        // Đảm bảo chỉ có một instance của managementFrame
        if (managementFrameInstance != null && managementFrameInstance.isDisplayable()) {
            managementFrameInstance.setTitle("Coffee Shop Management - User: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
            if (!managementFrameInstance.isVisible()) {
                managementFrameInstance.setVisible(true);
            }
            managementFrameInstance.toFront();
            managementFrameInstance.repaint(); // Yêu cầu vẽ lại
            return;
        }

        managementFrameInstance = new JFrame("Coffee Shop Management - User: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        // setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Lỗi: Không nên EXIT ở đây, mà là DISPOSE_ON_CLOSE để quay lại login
        managementFrameInstance.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Xử lý đóng cửa sổ bằng listener
        managementFrameInstance.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                // Có thể thêm xác nhận trước khi đóng
                // if (JOptionPane.showConfirmDialog(managementFrameInstance, ... ) == JOptionPane.YES_OPTION)
                // Hiện tại, khi đóng cửa sổ chính, ta sẽ đăng xuất và quay lại login
                performLogout();
            }
        });

        managementFrameInstance.setSize(1250, 850);
        managementFrameInstance.setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        // Thêm các Panel UI
        SimpleOrderUI orderPanel = new SimpleOrderUI();
        tabbedPane.addTab("Đơn Hàng", null, orderPanel, "Quản lý và tạo đơn hàng");

        SimpleIngredientUI ingredientPanel = new SimpleIngredientUI();
        tabbedPane.addTab("Nguyên Liệu", null, ingredientPanel, "Quản lý kho nguyên liệu");

        SimpleItemUI itemPanel = new SimpleItemUI();
        tabbedPane.addTab("Sản Phẩm", null, itemPanel, "Quản lý danh sách sản phẩm");

        SimpleStatisticsUI statsPanel = new SimpleStatisticsUI();
        tabbedPane.addTab("Thống Kê", null, statsPanel, "Xem thống kê và báo cáo");

        // Tab chỉ dành cho ADMIN hoặc có quyền cụ thể
        if (currentUser.getRole() == User.UserRole.ADMIN) {
            SimpleInventoryUI inventoryPanel = new SimpleInventoryUI();
            tabbedPane.addTab("QL Tồn Kho", null, inventoryPanel, "Quản lý chi tiết số lượng tồn kho");

            SimpleRecipeUI recipePanel = new SimpleRecipeUI();
            tabbedPane.addTab("QL Công Thức", null, recipePanel, "Quản lý công thức sản phẩm");

            SimpleStaffUI staffPanel = new SimpleStaffUI();
            tabbedPane.addTab("QL Nhân Viên", null, staffPanel, "Quản lý thông tin nhân viên");

            SimpleShiftUI shiftPanel = new SimpleShiftUI();
            tabbedPane.addTab("QL Ca Làm Việc", null, shiftPanel, "Quản lý ca làm việc");

            SimpleRotaUI rotaPanel = new SimpleRotaUI();
            tabbedPane.addTab("QL Lịch Làm Việc", null, rotaPanel, "Phân công lịch làm việc");

            SimpleUserManagementUI userManagementPanel = new SimpleUserManagementUI();
            tabbedPane.addTab("QL Users", null, userManagementPanel, "Quản lý tài khoản người dùng");
        } else { // STAFF
            // Staff có thể cần xem một số thông tin ở chế độ read-only hoặc hạn chế
            SimpleInventoryUI inventoryViewPanel = new SimpleInventoryUI(); // Cần logic read-only trong panel
            // Cần truyền vào SimpleInventoryUI là đang ở chế độ view cho staff
            // ví dụ: new SimpleInventoryUI(currentUser.getRole());
            // Và SimpleInventoryUI sẽ dựa vào đó để enable/disable các nút
            tabbedPane.addTab("Xem Tồn Kho", null, inventoryViewPanel, "Xem số lượng tồn kho");

            SimpleRecipeUI recipeViewPanel = new SimpleRecipeUI(); // Tương tự cho recipe
            tabbedPane.addTab("Xem Công Thức", null, recipeViewPanel, "Xem công thức sản phẩm");
        }

        managementFrameInstance.add(tabbedPane);

        JMenuBar menuBar = new JMenuBar();
        JMenu accountMenu = new JMenu("Tài Khoản (" + currentUser.getUsername() + ")");
        JMenuItem logoutItem = new JMenuItem("Đăng Xuất");
        logoutItem.addActionListener(e -> performLogout());
        accountMenu.add(logoutItem);
        menuBar.add(accountMenu);
        managementFrameInstance.setJMenuBar(menuBar);

        managementFrameInstance.setVisible(true);
    }

    private static void performLogout() {
        int confirm = JOptionPane.showConfirmDialog(managementFrameInstance, // Sử dụng instance hiện tại để xác nhận
                "Bạn có chắc chắn muốn đăng xuất?", "Xác Nhận Đăng Xuất",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            LoginFrame.logout(); // Reset user đang đăng nhập
            if (managementFrameInstance != null) {
                managementFrameInstance.dispose(); // Đóng cửa sổ quản lý
                managementFrameInstance = null;    // Reset instance
            }
            showLoginWindowAgain(); // Hiển thị lại cửa sổ đăng nhập
        }
    }

    public static void showLoginWindowAgain() {
        // Đảm bảo đóng cửa sổ quản lý nếu nó vẫn còn
        if (managementFrameInstance != null && managementFrameInstance.isDisplayable()) {
            managementFrameInstance.dispose();
            managementFrameInstance = null;
        }
        // Lấy hoặc tạo instance của LoginFrame và hiển thị
        LoginFrame login = LoginFrame.getInstance(); // Sử dụng Singleton pattern cho LoginFrame
        if (!login.isVisible()) {
            login.clearFieldsAndFocus(); // Thêm hàm này vào LoginFrame để xóa text cũ và focus username
            login.setVisible(true);
        }
        login.toFront();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Đoạn code này chỉ chạy một lần khi ứng dụng khởi động
            System.out.println("Khởi tạo ứng dụng, kiểm tra kết nối CSDL và user mẫu...");
            Connection testConn = null;
            boolean dbReady = false;
            try {
                testConn = DatabaseConnector.getConnection(); // Thử kết nối
                if (testConn != null && testConn.isValid(2)) {
                    System.out.println("Kết nối CSDL ban đầu thành công.");
                    dbReady = true;
                    // Chỉ tạo user mẫu nếu CSDL đã kết nối được
                    UserDAO userDAO = new UserDAO();
                    AuthService authService = new AuthService();

                    // Mật khẩu nên phức tạp hơn trong thực tế
                    String adminPass = "admin123";
                    String staffPass = "staff123";

                    if (!userDAO.findByUsername("admin").isPresent()) {
                        authService.registerUser("admin", adminPass, "Quản Trị", "Viên", "admin@coffeeshop.com", User.UserRole.ADMIN);
                        System.out.println("Tài khoản admin (admin/" + adminPass + ") đã được tạo.");
                    }
                    if (!userDAO.findByUsername("staff").isPresent()) {
                        // Nếu muốn liên kết User với Staff, User model cần trường staffId
                        // Và AuthService.registerUser cần tham số staffId
                        // Hiện tại, chúng ta chỉ tạo User với vai trò STAFF
                        authService.registerUser("staff", staffPass, "Nhân", "Viên", "staff@coffeeshop.com", User.UserRole.STAFF);
                        System.out.println("Tài khoản staff (staff/" + staffPass + ") đã được tạo.");
                    }
                } else {
                    throw new SQLException("Không thể thiết lập kết nối CSDL ban đầu hoặc kết nối không hợp lệ.");
                }
            } catch (DatabaseOperationException | IllegalArgumentException | SQLException e) {
                System.err.println("LỖI KHỞI TẠO: " + e.getMessage());
                e.printStackTrace(); // In stack trace để debug
                JOptionPane.showMessageDialog(null,
                        "Lỗi nghiêm trọng khi khởi tạo ứng dụng:\n" + e.getMessage()
                        + "\nVui lòng kiểm tra kết nối CSDL và thử lại.",
                        "Lỗi Khởi Tạo", JOptionPane.ERROR_MESSAGE);
                // Không cần đóng testConn ở đây, vì nếu lỗi, nó có thể đã null
                // DatabaseConnector.closeConnection() sẽ được gọi bởi shutdown hook
                return; // Thoát khỏi lambda nếu có lỗi nghiêm trọng
            }
            // Không đóng connection ở đây nữa (testConn). Nó được quản lý bởi DatabaseConnector.

            // Hiển thị cửa sổ đăng nhập
            System.out.println("Khởi chạy LoginFrame...");
            LoginFrame.getInstance().setVisible(true);
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Ứng dụng đang thoát, thực hiện đóng kết nối CSDL...");
            DatabaseConnector.closeConnection();
            System.out.println("Kết nối CSDL đã được đóng (nếu có).");
        }));
    }
}
