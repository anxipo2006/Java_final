package com.coffeeshop.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.coffeeshop.core.dao.ItemDAO;
import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.model.Item;

public class SimpleItemUI extends JPanel { // Thay đổi thành JPanel để có thể thêm vào JTabbedPane

    private ItemDAO itemDAO;
    private JTable itemTable;
    private DefaultTableModel tableModel;

    private JTextField txtItemId;
    private JTextField txtSku;
    private JTextField txtItemName;
    private JTextField txtItemCat;
    private JTextField txtItemSize;
    private JTextField txtItemPrice;

    public SimpleItemUI() {
        itemDAO = new ItemDAO();
        setLayout(new BorderLayout()); // Set layout cho JPanel này

        // --- Panel chứa bảng ---
        tableModel = new DefaultTableModel(new Object[]{"ID", "SKU", "Tên Item", "Danh Mục", "Kích Cỡ", "Giá"}, 0);
        itemTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(itemTable);
        add(scrollPane, BorderLayout.CENTER); // Thêm bảng vào vị trí trung tâm của JPanel

        // --- Panel chứa form nhập liệu và các nút ---
        JPanel formAndButtonPanel = new JPanel(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Item ID:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        txtItemId = new JTextField(15);
        formPanel.add(txtItemId, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        formPanel.add(new JLabel("SKU:"), gbc);
        gbc.gridx = 3;
        gbc.gridy = 0;
        txtSku = new JTextField(15);
        formPanel.add(txtSku, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Tên Item:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 3; // Kéo dài
        txtItemName = new JTextField(30);
        formPanel.add(txtItemName, gbc);
        gbc.gridwidth = 1; // Reset

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Danh mục:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        txtItemCat = new JTextField(15);
        formPanel.add(txtItemCat, gbc);

        gbc.gridx = 2;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Kích cỡ:"), gbc);
        gbc.gridx = 3;
        gbc.gridy = 2;
        txtItemSize = new JTextField(15);
        formPanel.add(txtItemSize, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Giá:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        txtItemPrice = new JTextField(15);
        formPanel.add(txtItemPrice, gbc);

        formAndButtonPanel.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnLoad = new JButton("Tải Dữ Liệu");
        JButton btnAdd = new JButton("Thêm");
        JButton btnUpdate = new JButton("Cập Nhật");
        JButton btnDelete = new JButton("Xóa");
        JButton btnClear = new JButton("Làm Mới Form");

        buttonPanel.add(btnLoad);
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnClear);

        formAndButtonPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(formAndButtonPanel, BorderLayout.SOUTH); // Thêm form và nút vào phía Nam của JPanel

        // --- Action Listeners ---
        btnLoad.addActionListener(e -> loadItems());
        btnAdd.addActionListener(e -> addItem());
        btnUpdate.addActionListener(e -> updateItem());
        btnDelete.addActionListener(e -> deleteItem());
        btnClear.addActionListener(e -> clearForm());

        itemTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && itemTable.getSelectedRow() != -1) {
                int selectedRow = itemTable.getSelectedRow();
                txtItemId.setText(tableModel.getValueAt(selectedRow, 0).toString());
                txtSku.setText(tableModel.getValueAt(selectedRow, 1).toString());
                txtItemName.setText(tableModel.getValueAt(selectedRow, 2).toString());
                txtItemCat.setText(tableModel.getValueAt(selectedRow, 3).toString());
                txtItemSize.setText(tableModel.getValueAt(selectedRow, 4) != null ? tableModel.getValueAt(selectedRow, 4).toString() : "");
                txtItemPrice.setText(tableModel.getValueAt(selectedRow, 5).toString());
                txtItemId.setEditable(false); // Không cho sửa ID khi đã chọn
                txtSku.setEditable(false); // SKU cũng thường không nên sửa trực tiếp khi đã tạo
            }
        });

        loadItems(); // Tải dữ liệu khi khởi động
    }

    private void loadItems() {
        try {
            tableModel.setRowCount(0); // Xóa dữ liệu cũ
            List<Item> items = itemDAO.getAllItems();
            for (Item item : items) {
                tableModel.addRow(new Object[]{
                    item.getItemId(), item.getSku(), item.getItemName(),
                    item.getItemCat(), item.getItemSize(), item.getItemPrice()
                });
            }
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu Items: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void addItem() {
        try {
            String itemId = txtItemId.getText().trim();
            String sku = txtSku.getText().trim();
            String name = txtItemName.getText().trim();
            String cat = txtItemCat.getText().trim();
            String size = txtItemSize.getText().trim();
            String priceStr = txtItemPrice.getText().trim();

            if (itemId.isEmpty() || sku.isEmpty() || name.isEmpty() || cat.isEmpty() || priceStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Item ID, SKU, Tên, Danh mục và Giá không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // item_size có thể là N/A hoặc null, nên không bắt buộc isEmpty

            // Kiểm tra ID và SKU đã tồn tại chưa (vì chúng là khóa/duy nhất)
            if (itemDAO.findById(itemId).isPresent()) {
                JOptionPane.showMessageDialog(this, "Item ID '" + itemId + "' đã tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (itemDAO.findBySku(sku).isPresent()) {
                JOptionPane.showMessageDialog(this, "SKU '" + sku + "' đã tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Item newItem = new Item(itemId, sku, name, cat, size.equals("N/A") || size.isEmpty() ? null : size, Double.parseDouble(priceStr));

            itemDAO.createItem(newItem);
            JOptionPane.showMessageDialog(this, "Thêm item thành công!");
            loadItems();
            clearForm();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Giá phải là số!", "Lỗi Định Dạng", JOptionPane.ERROR_MESSAGE);
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi thêm item: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void updateItem() {
        try {
            String itemId = txtItemId.getText().trim();
            if (itemId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một item từ bảng để cập nhật.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String sku = txtSku.getText().trim(); // SKU không cho sửa khi đã chọn từ bảng
            String name = txtItemName.getText().trim();
            String cat = txtItemCat.getText().trim();
            String size = txtItemSize.getText().trim();
            String priceStr = txtItemPrice.getText().trim();

            if (sku.isEmpty() || name.isEmpty() || cat.isEmpty() || priceStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "SKU, Tên, Danh mục và Giá không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Kiểm tra xem SKU có bị thay đổi thành một SKU đã tồn tại của item khác không
            // Ta chỉ kiểm tra nếu SKU trong text field khác với SKU của item đang được chọn (nếu có)
            int selectedRow = itemTable.getSelectedRow();
            if (selectedRow != -1) {
                String originalSkuInTable = tableModel.getValueAt(selectedRow, 1).toString();
                if (!sku.equals(originalSkuInTable)) { // Nếu SKU đã thay đổi
                    Optional<Item> itemWithNewSku = itemDAO.findBySku(sku);
                    if (itemWithNewSku.isPresent() && !itemWithNewSku.get().getItemId().equals(itemId)) {
                        JOptionPane.showMessageDialog(this, "SKU '" + sku + "' đã được sử dụng bởi item khác.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            } else { // Nếu không có item nào được chọn, và người dùng nhập SKU để update dựa trên ID
                Optional<Item> itemWithNewSku = itemDAO.findBySku(sku);
                if (itemWithNewSku.isPresent() && !itemWithNewSku.get().getItemId().equals(itemId)) {
                    JOptionPane.showMessageDialog(this, "SKU '" + sku + "' đã được sử dụng bởi item khác.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            Item itemToUpdate = new Item(itemId, sku, name, cat, size.equals("N/A") || size.isEmpty() ? null : size, Double.parseDouble(priceStr));

            if (itemDAO.updateItem(itemToUpdate)) {
                JOptionPane.showMessageDialog(this, "Cập nhật item thành công!");
                loadItems();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Cập nhật thất bại (ID có thể không tồn tại).", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Giá phải là số!", "Lỗi Định Dạng", JOptionPane.ERROR_MESSAGE);
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi cập nhật item: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void deleteItem() {
        String itemId = txtItemId.getText().trim();
        if (itemId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một item từ bảng để xóa.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa item ID: " + itemId + "?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION);

        if (confirmation == JOptionPane.YES_OPTION) {
            try {
                if (itemDAO.deleteItem(itemId)) {
                    JOptionPane.showMessageDialog(this, "Xóa item thành công!");
                    loadItems();
                    clearForm();
                } else {
                    // Thông báo lỗi cụ thể hơn đã được xử lý trong DAO (ví dụ ràng buộc khóa ngoại)
                    // Nên không cần thiết phải lặp lại ở đây, DAO sẽ throw exception
                    JOptionPane.showMessageDialog(this, "Xóa thất bại (ID có thể không tồn tại).", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (DatabaseOperationException e) {
                JOptionPane.showMessageDialog(this, "Lỗi khi xóa item: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void clearForm() {
        txtItemId.setText("");
        txtSku.setText("");
        txtItemName.setText("");
        txtItemCat.setText("");
        txtItemSize.setText("");
        txtItemPrice.setText("");
        txtItemId.setEditable(true);
        txtSku.setEditable(true);
        itemTable.clearSelection();
    }

    public static void main(String[] args) {
        // Test nhanh UI
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JFrame frame = new javax.swing.JFrame("Item Management");
            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            frame.add(new SimpleItemUI());
            frame.setSize(800, 600);
            frame.setVisible(true);
        });
    }
}
