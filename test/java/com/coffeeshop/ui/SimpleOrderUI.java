package com.coffeeshop.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField; // Để tạo Order ID mới
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

import com.coffeeshop.core.dao.ItemDAO;
import com.coffeeshop.core.dao.OrderDAO;
import com.coffeeshop.core.dao.OrderDAO.OrderLineView;
import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.model.Item;
import com.coffeeshop.core.model.OrderLine;
import com.coffeeshop.core.service.OrderService;

public class SimpleOrderUI extends JPanel {

    private final OrderDAO orderDAO;
    private final ItemDAO itemDAO;
    private final OrderService orderService; // Sử dụng OrderService cho logic nghiệp vụ

    private final JTable orderLinesTable;
    private final DefaultTableModel orderLinesTableModel;

    // Components cho việc tạo order
    private final JTextField txtCurrentOrderId;
    private final JTextField txtCustomerName;
    private final JComboBox<String> cbInOrOut;
    private final JComboBox<ItemWrapper> cbItemsForOrder;
    private final JSpinner spinnerQuantity;
    private final JButton btnAddToOrder;
    private final JButton btnNewOrderSession;
    private final JButton btnViewAllOrders;
    private final JTextField txtFilterOrderId;
    private final JButton btnFilterByOrderId;

    public SimpleOrderUI() {
        orderDAO = new OrderDAO();
        itemDAO = new ItemDAO();
        orderService = new OrderService(); // Khởi tạo OrderService

        setLayout(new BorderLayout(10, 10));

        // --- Panel hiển thị Order Lines ---
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Chi Tiết Đơn Hàng"));
        orderLinesTableModel = new DefaultTableModel(new Object[]{"RowID", "OrderID", "Thời Gian", "Item", "SKU", "SL", "Giá", "Thành Tiền", "Khách", "Loại"}, 0);
        orderLinesTable = new JTable(orderLinesTableModel);
        tablePanel.add(new JScrollPane(orderLinesTable), BorderLayout.CENTER);

        // --- Panel tạo và quản lý Order ---
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Tạo và Quản Lý Đơn Hàng"));

        // Panel thông tin Order hiện tại
        JPanel currentOrderInfoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcInfo = new GridBagConstraints();
        gbcInfo.insets = new Insets(2, 5, 2, 5);
        gbcInfo.anchor = GridBagConstraints.WEST;

        gbcInfo.gridx = 0;
        gbcInfo.gridy = 0;
        currentOrderInfoPanel.add(new JLabel("Order ID Hiện Tại:"), gbcInfo);
        gbcInfo.gridx = 1;
        gbcInfo.gridy = 0;
        txtCurrentOrderId = new JTextField(15);
        txtCurrentOrderId.setEditable(false);
        currentOrderInfoPanel.add(txtCurrentOrderId, gbcInfo);

        btnNewOrderSession = new JButton("Tạo Order Mới");
        gbcInfo.gridx = 2;
        gbcInfo.gridy = 0;
        currentOrderInfoPanel.add(btnNewOrderSession, gbcInfo);

        gbcInfo.gridx = 0;
        gbcInfo.gridy = 1;
        currentOrderInfoPanel.add(new JLabel("Tên Khách Hàng:"), gbcInfo);
        gbcInfo.gridx = 1;
        gbcInfo.gridy = 1;
        txtCustomerName = new JTextField(15);
        currentOrderInfoPanel.add(txtCustomerName, gbcInfo);

        gbcInfo.gridx = 0;
        gbcInfo.gridy = 2;
        currentOrderInfoPanel.add(new JLabel("Loại (In/Out):"), gbcInfo);
        gbcInfo.gridx = 1;
        gbcInfo.gridy = 2;
        cbInOrOut = new JComboBox<>(new String[]{"in", "out", "N/A"});
        currentOrderInfoPanel.add(cbInOrOut, gbcInfo);

        controlPanel.add(currentOrderInfoPanel, BorderLayout.NORTH);

        // Panel thêm item vào order
        JPanel addItemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addItemPanel.add(new JLabel("Sản phẩm:"));
        cbItemsForOrder = new JComboBox<>();
        loadItemsForOrderComboBox();
        addItemPanel.add(cbItemsForOrder);
        addItemPanel.add(new JLabel("Số lượng:"));
        spinnerQuantity = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1)); // Min 1, Max 100, Step 1
        addItemPanel.add(spinnerQuantity);
        btnAddToOrder = new JButton("Thêm vào Order");
        addItemPanel.add(btnAddToOrder);

        controlPanel.add(addItemPanel, BorderLayout.CENTER);

        // Panel Lọc và xem
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        filterPanel.add(new JLabel("Lọc theo Order ID:"));
        txtFilterOrderId = new JTextField(10);
        filterPanel.add(txtFilterOrderId);
        btnFilterByOrderId = new JButton("Lọc");
        filterPanel.add(btnFilterByOrderId);
        btnViewAllOrders = new JButton("Xem Tất Cả Orders");
        filterPanel.add(btnViewAllOrders);

        controlPanel.add(filterPanel, BorderLayout.SOUTH);

        // --- Bố cục chính ---
        add(tablePanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        // --- Listeners ---
        btnNewOrderSession.addActionListener(e -> startNewOrderSession());
        btnAddToOrder.addActionListener(e -> addItemToCurrentOrder());
        btnViewAllOrders.addActionListener(e -> loadAllOrderLines());
        btnFilterByOrderId.addActionListener(e -> loadOrderLinesFiltered());

        // Tải tất cả orders khi khởi động
        loadAllOrderLines();
        // startNewOrderSession(); // Bắt đầu với một session order mới
    }

    private void loadItemsForOrderComboBox() {
        try {
            List<Item> items = itemDAO.getAllItems();
            items.sort((i1, i2) -> i1.getItemName().compareToIgnoreCase(i2.getItemName()));
            cbItemsForOrder.removeAllItems();
            cbItemsForOrder.addItem(null); // Để có thể không chọn gì
            for (Item item : items) {
                cbItemsForOrder.addItem(new ItemWrapper(item));
            }
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách sản phẩm: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startNewOrderSession() {
        // Tạo một Order ID mới, ví dụ dùng UUID hoặc một chuỗi ngẫu nhiên đơn giản
        String newOrderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        txtCurrentOrderId.setText(newOrderId);
        txtCustomerName.setText(""); // Xóa tên khách hàng cũ
        cbInOrOut.setSelectedIndex(0); // Mặc định là "in"
        // Có thể xóa bảng chi tiết của order cũ nếu muốn
        // orderLinesTableModel.setRowCount(0);
        JOptionPane.showMessageDialog(this, "Đã bắt đầu phiên order mới: " + newOrderId, "Order Mới", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addItemToCurrentOrder() {
        String currentOrderId = txtCurrentOrderId.getText();
        if (currentOrderId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng bắt đầu một phiên order mới trước.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ItemWrapper selectedItemWrapper = (ItemWrapper) cbItemsForOrder.getSelectedItem();
        if (selectedItemWrapper == null || selectedItemWrapper.getItem() == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một sản phẩm.", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String custName = txtCustomerName.getText().trim();
        if (custName.isEmpty()) {
            custName = "Khách lẻ"; // Mặc định nếu không nhập
        }
        String inOrOut = cbInOrOut.getSelectedItem().toString();
        int quantity = (Integer) spinnerQuantity.getValue();

        try {
            OrderLine newOrderLine = new OrderLine(
                    currentOrderId,
                    new Timestamp(System.currentTimeMillis()), // Thời gian hiện tại
                    selectedItemWrapper.getItem().getItemId(),
                    quantity,
                    custName,
                    inOrOut
            );

            // Gọi OrderService để xử lý việc tạo order line và cập nhật inventory
            orderService.placeOrderLine(newOrderLine);

            JOptionPane.showMessageDialog(this, "Đã thêm '" + selectedItemWrapper.getItem().getItemName() + "' vào order " + currentOrderId, "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            loadOrderLinesFiltered(); // Tải lại chi tiết của order hiện tại

            // Reset lựa chọn item và số lượng
            cbItemsForOrder.setSelectedIndex(0);
            spinnerQuantity.setValue(1);

        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi thêm vào order: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalStateException e) { // Bắt lỗi từ OrderService nếu tồn kho không đủ
            JOptionPane.showMessageDialog(this, "Lỗi nghiệp vụ: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateTable(List<OrderLineView> views) {
        orderLinesTableModel.setRowCount(0); // Xóa dữ liệu cũ
        if (views.isEmpty()) {
            // JOptionPane.showMessageDialog(this, "Không tìm thấy order lines.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        for (OrderLineView olv : views) {
            orderLinesTableModel.addRow(new Object[]{
                olv.rowId,
                olv.orderId,
                olv.createdAtFormatted,
                olv.itemName,
                olv.itemSku,
                olv.quantity,
                String.format("%.2f", olv.itemPrice),
                String.format("%.2f", olv.subTotal),
                olv.custName,
                olv.inOrOut
            });
        }
    }

    private void loadAllOrderLines() {
        try {
            List<OrderLineView> allViews = orderDAO.getAllOrderLinesView();
            populateTable(allViews);
            txtFilterOrderId.setText(""); // Xóa filter
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải tất cả order lines: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadOrderLinesFiltered() {
        String filterId = txtFilterOrderId.getText().trim();
        if (filterId.isEmpty()) {
            // Nếu filter trống, có thể dùng order ID hiện tại hoặc không làm gì
            filterId = txtCurrentOrderId.getText().trim();
            if (filterId.isEmpty()) {
                loadAllOrderLines(); // Nếu cả hai đều trống, tải tất cả
                return;
            }
        }
        try {
            List<OrderLineView> filteredViews = orderDAO.getOrderLinesByOrderId(filterId);
            populateTable(filteredViews);
        } catch (DatabaseOperationException e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải order lines cho ID '" + filterId + "': " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Lớp Wrapper cho Item để hiển thị tên trong JComboBox
    private static class ItemWrapper {

        private Item item;

        public ItemWrapper(Item item) {
            this.item = item;
        }

        public Item getItem() {
            return item;
        }

        @Override
        public String toString() {
            return item != null ? (item.getItemName() + " (SKU: " + item.getSku() + ")") : "Chọn sản phẩm";
        }
    }

    // Main method để chạy UI
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JFrame frame = new javax.swing.JFrame("Coffee Shop Order UI");
            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            frame.add(new SimpleOrderUI());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
