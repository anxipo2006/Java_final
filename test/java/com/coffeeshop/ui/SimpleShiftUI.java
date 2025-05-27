package com.coffeeshop.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.coffeeshop.core.dao.ShiftDAO;
import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.model.Shift;
import com.coffeeshop.core.util.DatabaseConnector;

public class SimpleShiftUI extends JPanel {

    private ShiftDAO shiftDAO;
    private JTable shiftTable;
    private DefaultTableModel tableModel;

    private JTextField txtShiftId;
    private JComboBox<String> cbDayOfWeek;
    private JSpinner spinnerStartTime; // Dùng JSpinner cho Time
    private JSpinner spinnerEndTime;   // Dùng JSpinner cho Time
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm"); // Chỉ giờ:phút

    public SimpleShiftUI() {
        shiftDAO = new ShiftDAO();
        setLayout(new BorderLayout(10, 10));

        // --- Panel hiển thị bảng ---
        tableModel = new DefaultTableModel(new Object[]{"Shift ID", "Ngày trong Tuần", "Bắt Đầu", "Kết Thúc"}, 0);
        shiftTable = new JTable(tableModel);
        add(new JScrollPane(shiftTable), BorderLayout.CENTER);

        // --- Panel form nhập liệu ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Thông Tin Ca Làm Việc"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Shift ID:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        txtShiftId = new JTextField(15);
        formPanel.add(txtShiftId, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Ngày trong Tuần:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        cbDayOfWeek = new JComboBox<>(days);
        formPanel.add(cbDayOfWeek, gbc);

        // JSpinner cho Start Time
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Giờ Bắt Đầu (HH:mm):"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        SpinnerDateModel startTimeModel = new SpinnerDateModel(); // Default là ngày giờ hiện tại
        spinnerStartTime = new JSpinner(startTimeModel);
        JSpinner.DateEditor startTimeEditor = new JSpinner.DateEditor(spinnerStartTime, "HH:mm");
        spinnerStartTime.setEditor(startTimeEditor);
        formPanel.add(spinnerStartTime, gbc);

        // JSpinner cho End Time
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Giờ Kết Thúc (HH:mm):"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        SpinnerDateModel endTimeModel = new SpinnerDateModel();
        spinnerEndTime = new JSpinner(endTimeModel);
        JSpinner.DateEditor endTimeEditor = new JSpinner.DateEditor(spinnerEndTime, "HH:mm");
        spinnerEndTime.setEditor(endTimeEditor);
        formPanel.add(spinnerEndTime, gbc);

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
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(buttonPanel, gbc);

        add(formPanel, BorderLayout.SOUTH);

        // --- Listeners ---
        btnLoad.addActionListener(e -> loadShiftData());
        btnAdd.addActionListener(e -> addShift());
        btnUpdate.addActionListener(e -> updateShift());
        btnDelete.addActionListener(e -> deleteShift());
        btnClear.addActionListener(e -> clearForm());

        shiftTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && shiftTable.getSelectedRow() != -1) {
                int selectedRow = shiftTable.getSelectedRow();
                txtShiftId.setText(tableModel.getValueAt(selectedRow, 0).toString());
                cbDayOfWeek.setSelectedItem(tableModel.getValueAt(selectedRow, 1).toString());

                try {
                    // Chuyển đổi String HH:mm từ bảng thành Date để set cho JSpinner
                    Object startTimeObj = tableModel.getValueAt(selectedRow, 2);
                    Object endTimeObj = tableModel.getValueAt(selectedRow, 3);

                    if (startTimeObj != null) {
                        java.util.Date parsedStartTime = timeFormat.parse(startTimeObj.toString());
                        spinnerStartTime.setValue(parsedStartTime);
                    }
                    if (endTimeObj != null) {
                        java.util.Date parsedEndTime = timeFormat.parse(endTimeObj.toString());
                        spinnerEndTime.setValue(parsedEndTime);
                    }
                } catch (ParseException e) {
                    System.err.println("Lỗi parse time từ bảng: " + e.getMessage());
                    // Set về giá trị mặc định hoặc báo lỗi
                    spinnerStartTime.setValue(new java.util.Date()); // Hoặc một giá trị mặc định khác
                    spinnerEndTime.setValue(new java.util.Date());
                }
                txtShiftId.setEditable(false);
            }
        });

        loadShiftData();
    }

    private void loadShiftData() {
        try {
            tableModel.setRowCount(0);
            List<Shift> shifts = shiftDAO.getAllShifts();
            for (Shift shift : shifts) {
                tableModel.addRow(new Object[]{
                    shift.getShiftId(),
                    shift.getDayOfWeek(),
                    shift.getStartTime() != null ? timeFormat.format(shift.getStartTime()) : "N/A",
                    shift.getEndTime() != null ? timeFormat.format(shift.getEndTime()) : "N/A"
                });
            }
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách ca làm việc: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Time getSqlTimeFromSpinner(JSpinner spinner) {
        java.util.Date date = (java.util.Date) spinner.getValue();
        return new Time(date.getTime());
    }

    private void addShift() {
        try {
            String shiftId = txtShiftId.getText().trim();
            String dayOfWeek = cbDayOfWeek.getSelectedItem().toString();
            Time startTime = getSqlTimeFromSpinner(spinnerStartTime);
            Time endTime = getSqlTimeFromSpinner(spinnerEndTime);

            if (shiftId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Shift ID không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (startTime.after(endTime)) {
                JOptionPane.showMessageDialog(this, "Giờ bắt đầu không thể sau giờ kết thúc!", "Lỗi Logic", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (shiftDAO.findById(shiftId).isPresent()) {
                JOptionPane.showMessageDialog(this, "Shift ID '" + shiftId + "' đã tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Shift newShift = new Shift(shiftId, dayOfWeek, startTime, endTime);
            shiftDAO.createShift(newShift);

            JOptionPane.showMessageDialog(this, "Thêm ca làm việc thành công!");
            loadShiftData();
            clearForm();

        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi thêm ca làm việc: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateShift() {
        try {
            String shiftId = txtShiftId.getText().trim();
            if (shiftId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một ca làm việc từ bảng.", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String dayOfWeek = cbDayOfWeek.getSelectedItem().toString();
            Time startTime = getSqlTimeFromSpinner(spinnerStartTime);
            Time endTime = getSqlTimeFromSpinner(spinnerEndTime);

            if (startTime.after(endTime)) {
                JOptionPane.showMessageDialog(this, "Giờ bắt đầu không thể sau giờ kết thúc!", "Lỗi Logic", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Shift shiftToUpdate = new Shift(shiftId, dayOfWeek, startTime, endTime);

            if (shiftDAO.updateShift(shiftToUpdate)) {
                JOptionPane.showMessageDialog(this, "Cập nhật thông tin ca làm việc thành công!");
                loadShiftData();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Cập nhật thất bại (ID có thể không tồn tại).", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi cập nhật ca làm việc: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteShift() {
        String shiftId = txtShiftId.getText().trim();
        if (shiftId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một ca làm việc để xóa.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirmation = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa ca làm việc ID: " + shiftId + "?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION);

        if (confirmation == JOptionPane.YES_OPTION) {
            try {
                if (shiftDAO.deleteShift(shiftId)) {
                    JOptionPane.showMessageDialog(this, "Xóa ca làm việc thành công!");
                    loadShiftData();
                    clearForm();
                } else {
                    JOptionPane.showMessageDialog(this, "Xóa thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (DatabaseOperationException e) {
                JOptionPane.showMessageDialog(this, "Lỗi khi xóa ca làm việc: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearForm() {
        txtShiftId.setText("");
        cbDayOfWeek.setSelectedIndex(0); // Chọn ngày đầu tiên (Monday)
        // Đặt JSpinner về một thời gian mặc định, ví dụ 07:00 và 13:00
        try {
            spinnerStartTime.setValue(timeFormat.parse("07:00"));
            spinnerEndTime.setValue(timeFormat.parse("13:00"));
        } catch (ParseException e) {
            // Xử lý nếu parse mặc định bị lỗi
            spinnerStartTime.setValue(new java.util.Date());
            spinnerEndTime.setValue(new java.util.Date());
        }
        txtShiftId.setEditable(true);
        shiftTable.clearSelection();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame mainFrame = new JFrame("Coffee Shop Management");
            mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            mainFrame.setSize(1100, 800);
            mainFrame.setLocationRelativeTo(null);

            JTabbedPane tabbedPane = new JTabbedPane();

            SimpleIngredientUI ingredientPanel = new SimpleIngredientUI();
            tabbedPane.addTab("Nguyên Liệu", null, ingredientPanel, "Quản lý nguyên liệu");

            SimpleItemUI itemPanel = new SimpleItemUI();
            tabbedPane.addTab("Sản Phẩm (Items)", null, itemPanel, "Quản lý sản phẩm");

            SimpleInventoryUI inventoryPanel = new SimpleInventoryUI();
            tabbedPane.addTab("Tồn Kho (Inventory)", null, inventoryPanel, "Quản lý tồn kho");

            SimpleRecipeUI recipePanel = new SimpleRecipeUI();
            tabbedPane.addTab("Công Thức (Recipe)", null, recipePanel, "Quản lý công thức sản phẩm");

            SimpleOrderUI orderPanel = new SimpleOrderUI();
            tabbedPane.addTab("Đơn Hàng (Orders)", null, orderPanel, "Quản lý đơn hàng");

            SimpleStaffUI staffPanel = new SimpleStaffUI();
            tabbedPane.addTab("Nhân Viên (Staff)", null, staffPanel, "Quản lý nhân viên");

            SimpleShiftUI shiftPanel = new SimpleShiftUI(); // Thêm UI cho Shift
            tabbedPane.addTab("Ca Làm Việc (Shift)", null, shiftPanel, "Quản lý ca làm việc");

            mainFrame.add(tabbedPane);
            mainFrame.setVisible(true);
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Ứng dụng đang thoát, đóng kết nối CSDL...");
            DatabaseConnector.closeConnection();
        }));
    }
}
