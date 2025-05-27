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
import javax.swing.JPanel; // Đảm bảo JPanel được import
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.coffeeshop.core.dao.IngredientDAO;
import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.model.Ingredient;

public class SimpleIngredientUI extends JPanel { // Đã là JPanel

    private IngredientDAO ingredientDAO;
    private JTable ingredientTable;
    private DefaultTableModel tableModel;

    private JTextField txtIngId;
    private JTextField txtIngName;
    private JTextField txtIngWeight;
    private JTextField txtIngMeas;
    private JTextField txtIngPrice;

    public SimpleIngredientUI() {
        ingredientDAO = new IngredientDAO();

        // ---- XÓA HOẶC COMMENT OUT CÁC DÒNG NÀY ----
        // setTitle("Quản Lý Nguyên Liệu (Ingredients) - V2");
        // setSize(800, 600);
        // setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // JFrame.EXIT_ON_CLOSE không còn ý nghĩa
        // setLocationRelativeTo(null);
        // -------------------------------------------
        // Quan trọng: JPanel cần một LayoutManager
        setLayout(new BorderLayout()); // Ví dụ: BorderLayout (bạn đã có dòng này)

        tableModel = new DefaultTableModel(new Object[]{"ID", "Tên", "Trọng Lượng", "Đơn Vị", "Giá"}, 0);
        ingredientTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(ingredientTable);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("ID:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        txtIngId = new JTextField(20);
        formPanel.add(txtIngId, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Tên:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        txtIngName = new JTextField(20);
        formPanel.add(txtIngName, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Trọng lượng:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        txtIngWeight = new JTextField(10);
        formPanel.add(txtIngWeight, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Đơn vị:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        txtIngMeas = new JTextField(10);
        formPanel.add(txtIngMeas, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Giá:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 4;
        txtIngPrice = new JTextField(10);
        formPanel.add(txtIngPrice, gbc);

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

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(buttonPanel, gbc);

        btnLoad.addActionListener(e -> loadIngredients());
        btnAdd.addActionListener(e -> addIngredient());
        btnUpdate.addActionListener(e -> updateIngredient());
        btnDelete.addActionListener(e -> deleteIngredient());
        btnClear.addActionListener(e -> clearForm());

        ingredientTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && ingredientTable.getSelectedRow() != -1) {
                int selectedRow = ingredientTable.getSelectedRow();
                txtIngId.setText(tableModel.getValueAt(selectedRow, 0).toString());
                txtIngName.setText(tableModel.getValueAt(selectedRow, 1).toString());
                txtIngWeight.setText(tableModel.getValueAt(selectedRow, 2).toString());
                txtIngMeas.setText(tableModel.getValueAt(selectedRow, 3).toString());
                txtIngPrice.setText(tableModel.getValueAt(selectedRow, 4).toString());
                txtIngId.setEditable(false);
            }
        });

        // Thêm các component vào JPanel này (SimpleIngredientUI)
        add(scrollPane, BorderLayout.CENTER);
        add(formPanel, BorderLayout.SOUTH);

        loadIngredients();
    }

    private void loadIngredients() { // ... (giữ nguyên) ...
        try {
            tableModel.setRowCount(0); // Xóa dữ liệu cũ
            List<Ingredient> ingredients = ingredientDAO.getAllIngredients();
            for (Ingredient ing : ingredients) {
                tableModel.addRow(new Object[]{
                    ing.getIngId(), ing.getIngName(), ing.getIngWeight(),
                    ing.getIngMeas(), ing.getIngPrice()
                });
            }
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void addIngredient() { // ... (giữ nguyên) ...
        try {
            String id = txtIngId.getText().trim();
            String name = txtIngName.getText().trim();
            String weightStr = txtIngWeight.getText().trim();
            String meas = txtIngMeas.getText().trim();
            String priceStr = txtIngPrice.getText().trim();

            if (name.isEmpty() || weightStr.isEmpty() || meas.isEmpty() || priceStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên, trọng lượng, đơn vị và giá không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!id.isEmpty()) {
                Optional<Ingredient> existing = ingredientDAO.findById(id);
                if (existing.isPresent()) {
                    JOptionPane.showMessageDialog(this, "ID nguyên liệu '" + id + "' đã tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            Ingredient newIngredient = new Ingredient();
            newIngredient.setIngId(id.isEmpty() ? null : id);
            newIngredient.setIngName(name);
            newIngredient.setIngWeight(Integer.parseInt(weightStr));
            newIngredient.setIngMeas(meas);
            newIngredient.setIngPrice(Double.parseDouble(priceStr));

            // Sửa lỗi ở đây: phương thức createIngredient của bạn trong DAO có thể trả về Ingredient hoặc boolean
            // Giả sử nó trả về Ingredient đã được tạo (với ID nếu có)
            Ingredient createdIngredient = ingredientDAO.createIngredient(newIngredient);
            if (createdIngredient != null) { // Kiểm tra xem có tạo thành công không
                JOptionPane.showMessageDialog(this, "Thêm nguyên liệu thành công!");
                loadIngredients();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Thêm nguyên liệu thất bại (DAO không trả về đối tượng).", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Trọng lượng phải là số nguyên, Giá phải là số!", "Lỗi Định Dạng", JOptionPane.ERROR_MESSAGE);
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi thêm: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void updateIngredient() { // ... (giữ nguyên) ...
        try {
            String id = txtIngId.getText().trim();
            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một nguyên liệu từ bảng để cập nhật.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String name = txtIngName.getText().trim();
            String weightStr = txtIngWeight.getText().trim();
            String meas = txtIngMeas.getText().trim();
            String priceStr = txtIngPrice.getText().trim();

            if (name.isEmpty() || weightStr.isEmpty() || meas.isEmpty() || priceStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên, trọng lượng, đơn vị và giá không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Ingredient ingredientToUpdate = new Ingredient(id, name, Integer.parseInt(weightStr), meas, Double.parseDouble(priceStr));

            if (ingredientDAO.updateIngredient(ingredientToUpdate)) {
                JOptionPane.showMessageDialog(this, "Cập nhật nguyên liệu thành công!");
                loadIngredients();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Cập nhật thất bại (ID có thể không tồn tại).", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Trọng lượng phải là số nguyên, Giá phải là số!", "Lỗi Định Dạng", JOptionPane.ERROR_MESSAGE);
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi cập nhật: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void deleteIngredient() { // ... (giữ nguyên) ...
        String id = txtIngId.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một nguyên liệu từ bảng để xóa.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa nguyên liệu ID: " + id + "?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION);

        if (confirmation == JOptionPane.YES_OPTION) {
            try {
                if (ingredientDAO.deleteIngredient(id)) {
                    JOptionPane.showMessageDialog(this, "Xóa nguyên liệu thành công!");
                    loadIngredients();
                    clearForm();
                } else {
                    JOptionPane.showMessageDialog(this, "Xóa thất bại (ID có thể không tồn tại).", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (DatabaseOperationException e) {
                JOptionPane.showMessageDialog(this, "Lỗi khi xóa: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void clearForm() { // ... (giữ nguyên) ...
        txtIngId.setText("");
        txtIngName.setText("");
        txtIngWeight.setText("");
        txtIngMeas.setText("");
        txtIngPrice.setText("");
        txtIngId.setEditable(true);
        ingredientTable.clearSelection();
    }

    // ---- XÓA HOẶC COMMENT OUT HÀM MAIN NÀY ----
    // public static void main(String[] args) {
    // SimpleIngredientUI frame = new SimpleIngredientUI();
    // Lỗi sẽ xảy ra ở constructor nếu các lệnh JFrame chưa bị xóa
    // Và JPanel không thể setVisible(true) một mình như một cửa sổ
    // frame.setVisible(true);
    // }
    // ------------------------------------------
}
