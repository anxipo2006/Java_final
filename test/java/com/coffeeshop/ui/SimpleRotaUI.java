package com.coffeeshop.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Date; // Import User model
import java.text.ParseException; // Đảm bảo import đúng
import java.text.SimpleDateFormat;
import java.util.List; // Import exception mới
import java.util.Optional; // Import RotaService

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.coffeeshop.core.dao.RotaDAO;
import com.coffeeshop.core.dao.RotaDAO.RotaEntryView;
import com.coffeeshop.core.dao.ShiftDAO;
import com.coffeeshop.core.dao.StaffDAO;
import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.exception.RotaManagementException;
import com.coffeeshop.core.model.RotaEntry;
import com.coffeeshop.core.model.Shift;
import com.coffeeshop.core.model.Staff;
import com.coffeeshop.core.model.User;
import com.coffeeshop.core.service.RotaService;
import com.toedter.calendar.JDateChooser;

public class SimpleRotaUI extends JPanel {

    private RotaDAO rotaDAO;
    private StaffDAO staffDAO;
    private ShiftDAO shiftDAO;
    private RotaService rotaService; // Thêm RotaService

    private JTable rotaTable;
    private DefaultTableModel tableModel;

    // Components cho form Admin
    private JTextField txtAdminRotaId;
    private JDateChooser adminDateChooser;
    private JComboBox<ShiftWrapper> cbAdminShifts;
    private JComboBox<StaffWrapper> cbAdminStaff;
    private JButton btnAdminAddRotaEntry;
    private JButton btnAdminDeleteRotaEntry;
    private JButton btnAdminClearForm;

    // Components cho form Nhân viên đăng ký
    private JDateChooser staffRegistrationDateChooser;
    private JComboBox<ShiftWrapper> cbAvailableShiftsForStaff;
    private JButton btnStaffRegisterForShift;

    // Components chung cho Lọc và Xem
    private JDateChooser filterDateChooser;
    private JButton btnFilterByDate;
    private JButton btnViewAllRotas;

    private int selectedRotaRowId = -1; // Lưu row_id của dòng được chọn trong bảng

    public SimpleRotaUI() {
        rotaDAO = new RotaDAO();
        staffDAO = new StaffDAO();
        shiftDAO = new ShiftDAO();
        rotaService = new RotaService(); // Khởi tạo RotaService

        setLayout(new BorderLayout(10, 10));

        // --- Panel hiển thị bảng ---
        tableModel = new DefaultTableModel(new Object[]{"Row ID", "Rota ID", "Ngày", "Nhân Viên", "Ca ID", "Chi Tiết Ca"}, 0);
        rotaTable = new JTable(tableModel);
        add(new JScrollPane(rotaTable), BorderLayout.CENTER);

        // --- Panel điều khiển chính ở dưới ---
        JPanel southControlPanel = new JPanel(new BorderLayout(5, 5));

        // Panel chứa các form (Admin và Nhân viên)
        JPanel formsContainerPanel = new JPanel();
        formsContainerPanel.setLayout(new BoxLayout(formsContainerPanel, BoxLayout.Y_AXIS)); // Sắp xếp theo chiều dọc

        // --- Panel form cho Admin ---
        JPanel adminFormPanel = new JPanel(new GridBagLayout());
        adminFormPanel.setBorder(BorderFactory.createTitledBorder("Admin: Quản Lý Phân Công"));
        GridBagConstraints gbcAdmin = new GridBagConstraints();
        gbcAdmin.insets = new Insets(5, 5, 5, 5);
        gbcAdmin.fill = GridBagConstraints.HORIZONTAL;
        gbcAdmin.anchor = GridBagConstraints.WEST;

        int yAdmin = 0;
        gbcAdmin.gridx = 0;
        gbcAdmin.gridy = yAdmin;
        adminFormPanel.add(new JLabel("Rota ID:"), gbcAdmin);
        gbcAdmin.gridx = 1;
        gbcAdmin.gridy = yAdmin++;
        txtAdminRotaId = new JTextField(15);
        adminFormPanel.add(txtAdminRotaId, gbcAdmin);

        gbcAdmin.gridx = 0;
        gbcAdmin.gridy = yAdmin;
        adminFormPanel.add(new JLabel("Ngày:"), gbcAdmin);
        gbcAdmin.gridx = 1;
        gbcAdmin.gridy = yAdmin++;
        adminDateChooser = new JDateChooser();
        adminDateChooser.setDateFormatString("yyyy-MM-dd");
        adminDateChooser.setPreferredSize(new Dimension(150, adminDateChooser.getPreferredSize().height));
        adminFormPanel.add(adminDateChooser, gbcAdmin);

        gbcAdmin.gridx = 0;
        gbcAdmin.gridy = yAdmin;
        adminFormPanel.add(new JLabel("Ca làm việc:"), gbcAdmin);
        gbcAdmin.gridx = 1;
        gbcAdmin.gridy = yAdmin++;
        cbAdminShifts = new JComboBox<>();
        loadShiftsIntoComboBox(cbAdminShifts);
        adminFormPanel.add(cbAdminShifts, gbcAdmin);

        gbcAdmin.gridx = 0;
        gbcAdmin.gridy = yAdmin;
        adminFormPanel.add(new JLabel("Nhân viên:"), gbcAdmin);
        gbcAdmin.gridx = 1;
        gbcAdmin.gridy = yAdmin++;
        cbAdminStaff = new JComboBox<>();
        loadStaffIntoComboBox(cbAdminStaff);
        adminFormPanel.add(cbAdminStaff, gbcAdmin);

        JPanel adminActionButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnAdminAddRotaEntry = new JButton("Thêm/Sửa Phân Công"); // Gộp Thêm và Sửa
        btnAdminDeleteRotaEntry = new JButton("Xóa Phân Công");
        btnAdminClearForm = new JButton("Làm Mới Form (Admin)");
        adminActionButtonPanel.add(btnAdminAddRotaEntry);
        adminActionButtonPanel.add(btnAdminDeleteRotaEntry);
        adminActionButtonPanel.add(btnAdminClearForm);

        gbcAdmin.gridx = 0;
        gbcAdmin.gridy = yAdmin;
        gbcAdmin.gridwidth = 2;
        gbcAdmin.fill = GridBagConstraints.NONE;
        gbcAdmin.anchor = GridBagConstraints.CENTER;
        adminFormPanel.add(adminActionButtonPanel, gbcAdmin);

        // --- Panel form cho Nhân viên đăng ký ---
        JPanel staffRegisterPanel = new JPanel(new GridBagLayout());
        staffRegisterPanel.setBorder(BorderFactory.createTitledBorder("Nhân Viên: Đăng Ký Ca"));
        GridBagConstraints gbcStaff = new GridBagConstraints();
        gbcStaff.insets = new Insets(5, 5, 5, 5);
        gbcStaff.fill = GridBagConstraints.HORIZONTAL;
        gbcStaff.anchor = GridBagConstraints.WEST;

        int yStaff = 0;
        gbcStaff.gridx = 0;
        gbcStaff.gridy = yStaff;
        staffRegisterPanel.add(new JLabel("Chọn ngày đăng ký:"), gbcStaff);
        gbcStaff.gridx = 1;
        gbcStaff.gridy = yStaff++;
        staffRegistrationDateChooser = new JDateChooser();
        staffRegistrationDateChooser.setDateFormatString("yyyy-MM-dd");
        staffRegistrationDateChooser.setPreferredSize(new Dimension(150, staffRegistrationDateChooser.getPreferredSize().height));
        staffRegisterPanel.add(staffRegistrationDateChooser, gbcStaff);

        gbcStaff.gridx = 0;
        gbcStaff.gridy = yStaff;
        staffRegisterPanel.add(new JLabel("Ca còn trống:"), gbcStaff);
        gbcStaff.gridx = 1;
        gbcStaff.gridy = yStaff++;
        cbAvailableShiftsForStaff = new JComboBox<>();
        staffRegisterPanel.add(cbAvailableShiftsForStaff, gbcStaff);

        btnStaffRegisterForShift = new JButton("Đăng Ký Ca Đã Chọn");
        gbcStaff.gridx = 0;
        gbcStaff.gridy = yStaff;
        gbcStaff.gridwidth = 2;
        gbcStaff.fill = GridBagConstraints.NONE;
        gbcStaff.anchor = GridBagConstraints.CENTER;
        staffRegisterPanel.add(btnStaffRegisterForShift, gbcStaff);

        // Quyết định hiển thị panel nào dựa trên vai trò người dùng
        User currentUser = LoginFrame.getCurrentLoggedInUser();
        if (currentUser != null && currentUser.getRole() == User.UserRole.ADMIN) {
            formsContainerPanel.add(adminFormPanel);
        }
        // Nhân viên luôn thấy form đăng ký của mình (hoặc có thể ẩn nếu admin không muốn cho tự đăng ký)
        formsContainerPanel.add(staffRegisterPanel);

        southControlPanel.add(formsContainerPanel, BorderLayout.NORTH);

        // Panel lọc và xem chung
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Lọc và Xem Lịch"));
        filterPanel.add(new JLabel("Xem lịch ngày:"));
        filterDateChooser = new JDateChooser();
        filterDateChooser.setDateFormatString("yyyy-MM-dd");
        filterDateChooser.setPreferredSize(new Dimension(130, filterDateChooser.getPreferredSize().height));
        filterPanel.add(filterDateChooser);
        btnFilterByDate = new JButton("Lọc");
        filterPanel.add(btnFilterByDate);
        btnViewAllRotas = new JButton("Xem Tất Cả");
        filterPanel.add(btnViewAllRotas);

        southControlPanel.add(filterPanel, BorderLayout.CENTER); // Đặt panel lọc ở giữa hoặc dưới cùng

        add(southControlPanel, BorderLayout.SOUTH);

        // --- Listeners ---
        btnAdminAddRotaEntry.addActionListener(e -> adminAddOrUpdateRotaEntry());
        btnAdminDeleteRotaEntry.addActionListener(e -> adminDeleteRotaEntry());
        btnAdminClearForm.addActionListener(e -> clearAdminForm());

        btnStaffRegisterForShift.addActionListener(e -> staffPerformShiftRegistration());
        staffRegistrationDateChooser.addPropertyChangeListener("date", evt -> {
            if ("date".equals(evt.getPropertyName()) && evt.getNewValue() != null) {
                loadAvailableShiftsForSelectedDate();
            }
        });

        btnFilterByDate.addActionListener(e -> loadRotaDataFiltered());
        btnViewAllRotas.addActionListener(e -> loadAllRotaData());

        rotaTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && rotaTable.getSelectedRow() != -1) {
                int selectedRow = rotaTable.getSelectedRow();
                selectedRotaRowId = (int) tableModel.getValueAt(selectedRow, 0);

                // Nếu admin đang đăng nhập, điền form admin để sửa/xóa
                User cUser = LoginFrame.getCurrentLoggedInUser();
                if (cUser != null && cUser.getRole() == User.UserRole.ADMIN) {
                    txtAdminRotaId.setText(tableModel.getValueAt(selectedRow, 1).toString());
                    try {
                        // Ngày trong bảng có thể có (E) - tên thứ, cần loại bỏ trước khi parse
                        String dateStrFromTable = tableModel.getValueAt(selectedRow, 2).toString();
                        String dateOnlyStr = dateStrFromTable.split(" ")[0];
                        java.util.Date dateVal = new SimpleDateFormat("yyyy-MM-dd").parse(dateOnlyStr);
                        adminDateChooser.setDate(dateVal);
                    } catch (ParseException e) {
                        adminDateChooser.setDate(null);
                        System.err.println("Lỗi parse ngày từ bảng Rota: " + e.getMessage());
                    }

                    // Lấy staffId và shiftId từ DAO để đảm bảo đúng, thay vì từ table model có thể đã format
                    try {
                        Optional<RotaEntry> entryOpt = rotaDAO.findByRowId(selectedRotaRowId);
                        if (entryOpt.isPresent()) {
                            RotaEntry entry = entryOpt.get();
                            selectComboBoxItem(cbAdminStaff, entry.getStaffId(), StaffWrapper.class, s -> s.getStaff().getStaffId());
                            selectComboBoxItem(cbAdminShifts, entry.getShiftId(), ShiftWrapper.class, s -> s.getShift().getShiftId());
                        }
                    } catch (DatabaseOperationException e) {
                        System.err.println("Lỗi lấy chi tiết RotaEntry: " + e.getMessage());
                    }
                }
            }
        });

        loadAllRotaData();
        clearAdminForm();
        staffRegistrationDateChooser.setDate(new java.util.Date()); // Ngày hiện tại cho staff
        loadAvailableShiftsForSelectedDate(); // Tải ca trống ban đầu
    }

    private void loadShiftsIntoComboBox(JComboBox<ShiftWrapper> comboBox) {
        try {
            List<Shift> shifts = shiftDAO.getAllShifts();
            comboBox.removeAllItems();
            comboBox.addItem(new ShiftWrapper(null, "--- Chọn Ca ---")); // Item mặc định
            for (Shift shift : shifts) {
                comboBox.addItem(new ShiftWrapper(shift));
            }
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách ca làm việc: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadStaffIntoComboBox(JComboBox<StaffWrapper> comboBox) {
        try {
            List<Staff> staffList = staffDAO.getAllStaff();
            comboBox.removeAllItems();
            comboBox.addItem(new StaffWrapper(null, "--- Chọn Nhân Viên ---")); // Item mặc định
            for (Staff staff : staffList) {
                comboBox.addItem(new StaffWrapper(staff));
            }
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách nhân viên: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private <T, W> void selectComboBoxItem(JComboBox<W> comboBox, String idToSelect, Class<W> wrapperClass, java.util.function.Function<W, String> idExtractor) {
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            W wrapper = comboBox.getItemAt(i);
            if (wrapper != null && wrapperClass.isInstance(wrapper)) {
                // Kiểm tra xem wrapper có trả về đối tượng null không trước khi gọi idExtractor
                Object innerObject = null;
                if (wrapper instanceof StaffWrapper) {
                    innerObject = ((StaffWrapper) wrapper).getStaff();
                } else if (wrapper instanceof ShiftWrapper) {
                    innerObject = ((ShiftWrapper) wrapper).getShift();
                }

                if (innerObject != null && idExtractor.apply(wrapper).equals(idToSelect)) {
                    comboBox.setSelectedIndex(i);
                    return;
                }
            }
        }
        if (comboBox.getItemCount() > 0) {
            comboBox.setSelectedIndex(0); // Nếu không tìm thấy, chọn item đầu tiên (thường là "--- Chọn ---")

        }
    }

    private void populateTable(List<RotaEntryView> views) {
        tableModel.setRowCount(0);
        for (RotaEntryView rv : views) {
            tableModel.addRow(new Object[]{
                rv.rowId,
                rv.rotaId,
                rv.dateFormatted,
                rv.staffName,
                rv.shiftId,
                rv.shiftInfo
            });
        }
    }

    private void loadAllRotaData() {
        try {
            List<RotaEntryView> rotaList = rotaDAO.getAllRotaViews();
            populateTable(rotaList);
            if (filterDateChooser != null) {
                filterDateChooser.setDate(null);
            }
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu Rota: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadRotaDataFiltered() {
        java.util.Date selectedUtilDate = filterDateChooser.getDate();
        if (selectedUtilDate == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một ngày để lọc.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Date sqlDate = new Date(selectedUtilDate.getTime());
        try {
            List<RotaEntryView> rotaList = rotaDAO.getRotaViewsByDate(sqlDate);
            populateTable(rotaList);
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu Rota cho ngày " + sqlDate + ": " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void adminAddOrUpdateRotaEntry() {
        try {
            String rotaId = txtAdminRotaId.getText().trim();
            java.util.Date utilDate = adminDateChooser.getDate();
            ShiftWrapper selectedShiftWrapper = (ShiftWrapper) cbAdminShifts.getSelectedItem();
            StaffWrapper selectedStaffWrapper = (StaffWrapper) cbAdminStaff.getSelectedItem();

            if (rotaId.isEmpty()) {
                rotaId = "AUTO_RT_" + System.currentTimeMillis() % 10000;
            }
            if (utilDate == null || selectedShiftWrapper == null || selectedShiftWrapper.getShift() == null
                    || selectedStaffWrapper == null || selectedStaffWrapper.getStaff() == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ thông tin (Ngày, Ca, Nhân Viên).", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Date sqlDate = new Date(utilDate.getTime());
            String shiftId = selectedShiftWrapper.getShift().getShiftId();
            String staffId = selectedStaffWrapper.getStaff().getStaffId();
            RotaEntry entry = new RotaEntry(rotaId, sqlDate, shiftId, staffId);

            if (selectedRotaRowId != -1) { // Chế độ cập nhật
                entry.setRowId(selectedRotaRowId);
                if (rotaDAO.updateRotaEntry(entry)) { // Giả sử updateRotaEntry đã được sửa để nhận RotaEntry
                    JOptionPane.showMessageDialog(this, "Cập nhật phân công thành công!");
                } else {
                    JOptionPane.showMessageDialog(this, "Cập nhật phân công thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } else { // Chế độ thêm mới
                // Sử dụng RotaService để thêm, vì nó có kiểm tra logic (nếu bạn đã implement cho admin)
                // Hoặc gọi trực tiếp DAO nếu admin có toàn quyền và không cần kiểm tra phức tạp như nhân viên tự đăng ký
                rotaService.registerForShift(staffId, shiftId, sqlDate, rotaId); // RotaService sẽ xử lý các kiểm tra
                // rotaDAO.createRotaEntry(entry); // Nếu gọi DAO trực tiếp
                JOptionPane.showMessageDialog(this, "Thêm phân công thành công!");
            }
            loadAllRotaData();
            clearAdminForm();

        } catch (DatabaseOperationException | RotaManagementException e) { // Bắt cả RotaManagementException
            JOptionPane.showMessageDialog(this, "Lỗi khi xử lý phân công: " + e.getMessage(), "Lỗi Hệ Thống", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void adminDeleteRotaEntry() {
        if (selectedRotaRowId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một phân công từ bảng để xóa.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirmation = JOptionPane.showConfirmDialog(this,
                "Admin: Bạn có chắc chắn muốn xóa phân công này (Row ID: " + selectedRotaRowId + ")?",
                "Xác Nhận Xóa", JOptionPane.YES_NO_OPTION);

        if (confirmation == JOptionPane.YES_OPTION) {
            try {
                if (rotaDAO.deleteRotaEntry(selectedRotaRowId)) {
                    JOptionPane.showMessageDialog(this, "Xóa phân công thành công!");
                    loadAllRotaData();
                    clearAdminForm();
                } else {
                    JOptionPane.showMessageDialog(this, "Xóa thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (DatabaseOperationException e) {
                JOptionPane.showMessageDialog(this, "Lỗi khi xóa phân công: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearAdminForm() {
        txtAdminRotaId.setText("");
        adminDateChooser.setDate(new java.util.Date());
        if (cbAdminShifts.getItemCount() > 0) {
            cbAdminShifts.setSelectedIndex(0);
        }
        if (cbAdminStaff.getItemCount() > 0) {
            cbAdminStaff.setSelectedIndex(0);
        }
        selectedRotaRowId = -1;
        rotaTable.clearSelection();
    }

    private void loadAvailableShiftsForSelectedDate() {
        cbAvailableShiftsForStaff.removeAllItems();
        java.util.Date selectedUtilDate = staffRegistrationDateChooser.getDate();
        if (selectedUtilDate == null) {
            cbAvailableShiftsForStaff.addItem(new ShiftWrapper(null, "Chọn ngày để xem ca"));
            return;
        }
        Date sqlDate = new Date(selectedUtilDate.getTime());
        try {
            List<Shift> available = rotaService.getAvailableShiftsForDate(sqlDate);
            if (available.isEmpty()) {
                cbAvailableShiftsForStaff.addItem(new ShiftWrapper(null, "Không có ca trống"));
            } else {
                cbAvailableShiftsForStaff.addItem(new ShiftWrapper(null, "--- Chọn Ca Còn Trống ---"));
                for (Shift shift : available) {
                    cbAvailableShiftsForStaff.addItem(new ShiftWrapper(shift));
                }
            }
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách ca trống: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void staffPerformShiftRegistration() {
        User currentUser = LoginFrame.getCurrentLoggedInUser();
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập để đăng ký ca.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        java.util.Date utilDate = staffRegistrationDateChooser.getDate();
        ShiftWrapper selectedShiftWrapper = (ShiftWrapper) cbAvailableShiftsForStaff.getSelectedItem();

        if (utilDate == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ngày đăng ký.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selectedShiftWrapper == null || selectedShiftWrapper.getShift() == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một ca làm việc còn trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Date sqlDate = new Date(utilDate.getTime());
        String shiftId = selectedShiftWrapper.getShift().getShiftId();
        // Giả sử User.username là staff_id, nếu không bạn cần cách lấy staff_id từ currentUser
        String staffId = currentUser.getUsername();

        try {
            rotaService.registerForShift(staffId, shiftId, sqlDate, null); // Rota ID sẽ tự tạo nếu null
            JOptionPane.showMessageDialog(this, "Đăng ký ca làm việc thành công!", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            loadAllRotaData();
            loadAvailableShiftsForSelectedDate();
        } catch (RotaManagementException | DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Đăng ký ca thất bại: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lớp Wrapper để hiển thị trong JComboBox
    private static class ShiftWrapper {

        private Shift shift;
        private final String displayText;

        public ShiftWrapper(Shift shift) {
            this.shift = shift;
            this.displayText = (shift != null) ? shift.toString() : "--- Chọn Ca ---";
        }

        public ShiftWrapper(Shift shift, String customText) { // Cho phép custom text
            this.shift = shift;
            this.displayText = customText;
        }

        public Shift getShift() {
            return shift;
        }

        @Override
        public String toString() {
            return displayText;
        }
    }

    private static class StaffWrapper {

        private final Staff staff;
        private String displayText;

        public StaffWrapper(Staff staff) {
            this.staff = staff;
            this.displayText = (staff != null) ? (staff.getFullName() + " (" + staff.getStaffId() + ")") : "--- Chọn Nhân Viên ---";
        }

        public StaffWrapper(Staff staff, String customText) {
            this.staff = staff;
            this.displayText = customText;
        }

        public Staff getStaff() {
            return staff;
        }

        @Override
        public String toString() {
            return displayText;
        }
    }

    public static void main(String[] args) {
        // Test UI
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JFrame frame = new javax.swing.JFrame("Quản Lý Rota");
            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            frame.add(new SimpleRotaUI());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
