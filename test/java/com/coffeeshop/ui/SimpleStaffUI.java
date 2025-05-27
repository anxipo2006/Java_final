package com.coffeeshop.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.coffeeshop.core.dao.StaffDAO;
import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.model.Staff;

public class SimpleStaffUI extends JPanel {

    private StaffDAO staffDAO;
    private JTable staffTable;
    private DefaultTableModel tableModel;

    private JTextField txtStaffId;
    private JTextField txtFirstName;
    private JTextField txtLastName;
    private JTextField txtPosition;
    private JTextField txtSalaryPerHour;

    public SimpleStaffUI() {
        staffDAO = new StaffDAO();
        setLayout(new BorderLayout(10, 10));

        // --- Panel hiển thị bảng ---
        tableModel = new DefaultTableModel(new Object[]{"Staff ID", "Họ", "Tên", "Chức Vụ", "Lương/giờ"}, 0);
        staffTable = new JTable(tableModel);
        add(new JScrollPane(staffTable), BorderLayout.CENTER);

        // --- Panel form nhập liệu ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Thông Tin Nhân Viên"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Staff ID:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        txtStaffId = new JTextField(15);
        formPanel.add(txtStaffId, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Họ:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        txtLastName = new JTextField(15);
        formPanel.add(txtLastName, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Tên:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        txtFirstName = new JTextField(15);
        formPanel.add(txtFirstName, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Chức vụ:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        txtPosition = new JTextField(15);
        formPanel.add(txtPosition, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Lương/giờ:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 4;
        txtSalaryPerHour = new JTextField(10);
        formPanel.add(txtSalaryPerHour, gbc);

        // --- Panel nút ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnLoad = new JButton("Tải DS");
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
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(buttonPanel, gbc);

        add(formPanel, BorderLayout.SOUTH);

        // --- Listeners ---
        btnLoad.addActionListener(e -> loadStaffData());
        btnAdd.addActionListener(e -> addStaff());
        btnUpdate.addActionListener(e -> updateStaff());
        btnDelete.addActionListener(e -> deleteStaff());
        btnClear.addActionListener(e -> clearForm());

        staffTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && staffTable.getSelectedRow() != -1) {
                int selectedRow = staffTable.getSelectedRow();
                txtStaffId.setText(tableModel.getValueAt(selectedRow, 0).toString());
                txtLastName.setText(tableModel.getValueAt(selectedRow, 1).toString());
                txtFirstName.setText(tableModel.getValueAt(selectedRow, 2).toString());
                txtPosition.setText(tableModel.getValueAt(selectedRow, 3).toString());
                txtSalaryPerHour.setText(tableModel.getValueAt(selectedRow, 4).toString());
                txtStaffId.setEditable(false);
            }
        });

        loadStaffData();
    }

    private void loadStaffData() {
        try {
            tableModel.setRowCount(0);
            List<Staff> staffList = staffDAO.getAllStaff();
            for (Staff staff : staffList) {
                tableModel.addRow(new Object[]{
                    staff.getStaffId(),
                    staff.getLastName(),
                    staff.getFirstName(),
                    staff.getPosition(),
                    staff.getSalaryPerHour()
                });
            }
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách nhân viên: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addStaff() {
        try {
            String staffId = txtStaffId.getText().trim();
            String firstName = txtFirstName.getText().trim();
            String lastName = txtLastName.getText().trim();
            String position = txtPosition.getText().trim();
            String salaryStr = txtSalaryPerHour.getText().trim();

            if (staffId.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || position.isEmpty() || salaryStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tất cả các trường không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (staffDAO.findById(staffId).isPresent()) {
                JOptionPane.showMessageDialog(this, "Staff ID '" + staffId + "' đã tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double salary = Double.parseDouble(salaryStr);
            Staff newStaff = new Staff(staffId, firstName, lastName, position, salary);

            staffDAO.createStaff(newStaff);
            JOptionPane.showMessageDialog(this, "Thêm nhân viên thành công!");
            loadStaffData();
            clearForm();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Lương/giờ phải là một số!", "Lỗi Định Dạng", JOptionPane.ERROR_MESSAGE);
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi thêm nhân viên: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateStaff() {
        try {
            String staffId = txtStaffId.getText().trim();
            if (staffId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một nhân viên từ bảng.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String firstName = txtFirstName.getText().trim();
            String lastName = txtLastName.getText().trim();
            String position = txtPosition.getText().trim();
            String salaryStr = txtSalaryPerHour.getText().trim();

            if (firstName.isEmpty() || lastName.isEmpty() || position.isEmpty() || salaryStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên, Họ, Chức vụ và Lương không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double salary = Double.parseDouble(salaryStr);
            Staff staffToUpdate = new Staff(staffId, firstName, lastName, position, salary);

            if (staffDAO.updateStaff(staffToUpdate)) {
                JOptionPane.showMessageDialog(this, "Cập nhật thông tin nhân viên thành công!");
                loadStaffData();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Cập nhật thất bại (ID có thể không tồn tại).", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Lương/giờ phải là một số!", "Lỗi Định Dạng", JOptionPane.ERROR_MESSAGE);
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi cập nhật nhân viên: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteStaff() {
        String staffId = txtStaffId.getText().trim();
        if (staffId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một nhân viên để xóa.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa nhân viên ID: " + staffId + "?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION);

        if (confirmation == JOptionPane.YES_OPTION) {
            try {
                if (staffDAO.deleteStaff(staffId)) {
                    JOptionPane.showMessageDialog(this, "Xóa nhân viên thành công!");
                    loadStaffData();
                    clearForm();
                } else {
                    // Thông báo lỗi cụ thể hơn có thể đã được ném từ DAO
                    JOptionPane.showMessageDialog(this, "Xóa thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (DatabaseOperationException e) {
                JOptionPane.showMessageDialog(this, "Lỗi khi xóa nhân viên: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearForm() {
        txtStaffId.setText("");
        txtFirstName.setText("");
        txtLastName.setText("");
        txtPosition.setText("");
        txtSalaryPerHour.setText("");
        txtStaffId.setEditable(true);
        staffTable.clearSelection();
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JFrame frame = new javax.swing.JFrame("Quản lý Nhân viên");
            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            frame.add(new SimpleStaffUI());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
