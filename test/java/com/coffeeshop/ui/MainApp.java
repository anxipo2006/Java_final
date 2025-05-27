package com.coffeeshop.ui; // Hoặc com.coffeeshop nếu bạn muốn đặt nó ở đó

import javax.swing.SwingUtilities;

import com.coffeeshop.core.util.DatabaseConnector;

public class MainApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimpleIngredientUI ui = new SimpleIngredientUI();
            ui.setVisible(true);
        });

        // Thêm một shutdown hook để đóng kết nối CSDL khi ứng dụng thoát
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Ứng dụng đang thoát, đóng kết nối CSDL...");
            DatabaseConnector.closeConnection();
        }));

    }
}
