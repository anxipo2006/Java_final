package com.coffeeshop.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.coffeeshop.core.dao.IngredientDAO;
import com.coffeeshop.core.dao.InventoryDAO;
import com.coffeeshop.core.dao.InventoryDAO.InventoryView;
import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.model.Ingredient;
import com.coffeeshop.core.util.DatabaseConnector;

public class SimpleInventoryUI extends JPanel {

    private final InventoryDAO inventoryDAO;
    private final IngredientDAO ingredientDAO;
    private final JTable inventoryTable;
    private final DefaultTableModel tableModel;
    private final JComboBox<IngredientWrapper> cbIngredients;
    private final JTextField txtQuantityChange;
    private final JButton btnUpdateQuantity;

    public SimpleInventoryUI() {
        inventoryDAO = new InventoryDAO();
        ingredientDAO = new IngredientDAO();
        setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(
                new Object[]{"Inv ID", "Ing ID", "Tên Nguyên Liệu", "Số Lượng Hiện Tại", "Đơn Vị"}, 0);
        inventoryTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Nguyên liệu:"), gbc);

        gbc.gridx = 1;
        cbIngredients = new JComboBox<>();
        loadIngredientsIntoComboBox();
        formPanel.add(cbIngredients, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Thay đổi số lượng (+/-):"), gbc);

        gbc.gridx = 1;
        txtQuantityChange = new JTextField(10);
        formPanel.add(txtQuantityChange, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnLoad = new JButton("Tải Kho");
        btnUpdateQuantity = new JButton("Cập Nhật Kho (Upsert)");
        buttonPanel.add(btnLoad);
        buttonPanel.add(btnUpdateQuantity);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(buttonPanel, gbc);

        add(formPanel, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> loadInventoryTable());
        btnUpdateQuantity.addActionListener(e -> updateInventoryByUpsert());

        loadInventoryTable();
    }

    private void loadIngredientsIntoComboBox() {
        try {
            List<Ingredient> ingredients = ingredientDAO.getAllIngredients();
            ingredients.sort((i1, i2) -> i1.getIngName().compareToIgnoreCase(i2.getIngName()));
            cbIngredients.removeAllItems();
            for (Ingredient ing : ingredients) {
                cbIngredients.addItem(new IngredientWrapper(ing));
            }
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách nguyên liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadInventoryTable() {
        try {
            tableModel.setRowCount(0);
            List<InventoryView> inventoryList = inventoryDAO.getAllInventoryWithIngredientNames();
            for (InventoryView iv : inventoryList) {
                tableModel.addRow(new Object[]{
                    iv.invId,
                    iv.ingId,
                    iv.ingredientName,
                    iv.quantity,
                    iv.ingMeas
                });
            }
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu Inventory: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void updateInventoryByUpsert() {
        IngredientWrapper selectedWrapper = (IngredientWrapper) cbIngredients.getSelectedItem();
        if (selectedWrapper == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một nguyên liệu.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String ingId = selectedWrapper.getIngredient().getIngId();
        String quantityChangeStr = txtQuantityChange.getText().trim();

        if (quantityChangeStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số lượng thay đổi.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int quantityChange = Integer.parseInt(quantityChangeStr);

            if (inventoryDAO.upsertInventoryQuantity(ingId, quantityChange)) {
                JOptionPane.showMessageDialog(this, "Cập nhật kho cho '" + selectedWrapper.getIngredient().getIngName() + "' thành công!");
                loadInventoryTable();
                txtQuantityChange.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Cập nhật kho thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số lượng thay đổi phải là một số nguyên!", "Lỗi Định Dạng", JOptionPane.ERROR_MESSAGE);
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi cập nhật kho: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private static class IngredientWrapper {

        private Ingredient ingredient;

        public IngredientWrapper(Ingredient ingredient) {
            this.ingredient = ingredient;
        }

        public Ingredient getIngredient() {
            return ingredient;
        }

        @Override
        public String toString() {
            return ingredient.getIngName() + " (ID: " + ingredient.getIngId() + ")";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Inventory Manager");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 700);
            frame.setLocationRelativeTo(null);
            frame.setContentPane(new SimpleInventoryUI());
            frame.setVisible(true);
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Đóng kết nối CSDL...");
            DatabaseConnector.closeConnection();
        }));
    }
}
