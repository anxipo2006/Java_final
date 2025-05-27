package com.coffeeshop.core.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.model.OrderLine; // java.sql.Date
import com.coffeeshop.core.model.dto.DailyRevenueDTO;
import com.coffeeshop.core.model.dto.TopSellingItemDTO;
import com.coffeeshop.core.util.DatabaseConnector;

public class OrderDAO {

    // --- DTO OrderLineView ---
    public static class OrderLineView {

        public int rowId;
        public String orderId;
        public String createdAtFormatted;
        public String itemId;
        public String itemName;
        public String itemSku;
        public int quantity;
        public double itemPrice;
        public double subTotal;
        public String custName;
        public String inOrOut;

        public OrderLineView(int rowId, String orderId, Timestamp createdAt, String itemId, String itemName, String itemSku, int quantity, double itemPrice, String custName, String inOrOut) {
            this.rowId = rowId;
            this.orderId = orderId;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(TimeZone.getDefault());
            this.createdAtFormatted = sdf.format(new java.util.Date(createdAt.getTime()));
            this.itemId = itemId;
            this.itemName = itemName;
            this.itemSku = itemSku;
            this.quantity = quantity;
            this.itemPrice = itemPrice;
            this.subTotal = this.quantity * this.itemPrice;
            this.custName = custName;
            this.inOrOut = (inOrOut == null || inOrOut.trim().isEmpty()) ? "N/A" : inOrOut;
        }
    }

    private OrderLineView mapRowToOrderLineView(ResultSet rs) throws SQLException {
        return new OrderLineView(
                rs.getInt("row_id"), rs.getString("order_id"), rs.getTimestamp("created_at"),
                rs.getString("item_id"), rs.getString("item_name"), rs.getString("sku"),
                rs.getInt("o_quantity"), rs.getDouble("item_price"), rs.getString("cust_name"),
                rs.getString("in_or_out"));
    }

    // --- Phiên bản được OrderService sử dụng, nhận Connection ---
    public OrderLine createOrderLine(OrderLine orderLine, Connection conn) throws DatabaseOperationException {
        String sql = "INSERT INTO orders (order_id, created_at, item_id, quantity, cust_name, in_or_out) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, orderLine.getOrderId());
            pstmt.setTimestamp(2, orderLine.getCreatedAt());
            pstmt.setString(3, orderLine.getItemId());
            pstmt.setInt(4, orderLine.getQuantity());
            pstmt.setString(5, orderLine.getCustName());
            pstmt.setString(6, orderLine.getInOrOut());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseOperationException("Tạo order line thất bại (với conn).");
            }
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    orderLine.setRowId(generatedKeys.getInt(1));
                } else {
                    System.out.println("Thông báo: Không lấy được row_id tự tăng cho order line mới.");
                }
            }
            return orderLine;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi khi tạo order line (với conn): " + e.getMessage(), e);
        }
    }

    // --- Các phương thức public khác tự quản lý connection ---
    public OrderLine createOrderLine(OrderLine orderLine) throws DatabaseOperationException {
        // Kiểm tra FK item_id
        ItemDAO itemDAO = new ItemDAO(); // Sẽ tự lấy connection
        if (!itemDAO.findById(orderLine.getItemId()).isPresent()) {
            throw new DatabaseOperationException("Không thể tạo order line: Item ID '" + orderLine.getItemId() + "' không tồn tại.");
        }
        if (orderLine.getQuantity() <= 0) {
            throw new DatabaseOperationException("Số lượng item trong order phải lớn hơn 0.");
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            return createOrderLine(orderLine, conn);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi khi lấy connection cho OrderDAO.createOrderLine (public)", e);
        }
    }

    public List<OrderLineView> getAllOrderLinesView() throws DatabaseOperationException {
        List<OrderLineView> views = new ArrayList<>();
        String sql = "SELECT o.row_id, o.order_id, o.created_at, o.item_id, it.item_name, it.sku, "
                + "o.quantity AS o_quantity, it.item_price, o.cust_name, o.in_or_out "
                + "FROM orders o "
                + "JOIN items it ON o.item_id = it.item_id "
                + "ORDER BY o.created_at DESC, o.order_id, o.row_id";
        try (Connection conn = DatabaseConnector.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                views.add(mapRowToOrderLineView(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi lấy tất cả order lines: " + e.getMessage(), e);
        }
        return views;
    }

    public List<OrderLineView> getOrderLinesByOrderId(String orderId) throws DatabaseOperationException {
        List<OrderLineView> views = new ArrayList<>();
        String sql = "SELECT o.row_id, o.order_id, o.created_at, o.item_id, it.item_name, it.sku, "
                + "o.quantity AS o_quantity, it.item_price, o.cust_name, o.in_or_out "
                + "FROM orders o "
                + "JOIN items it ON o.item_id = it.item_id "
                + "WHERE o.order_id = ? "
                + "ORDER BY o.row_id";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                views.add(mapRowToOrderLineView(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi lấy order lines cho order_id " + orderId + ": " + e.getMessage(), e);
        }
        return views;
    }

    public boolean deleteOrderLine(int rowId) throws DatabaseOperationException {
        String sql = "DELETE FROM orders WHERE row_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, rowId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi xóa order line row_id: " + rowId, e);
        }
    }

    // --- Phương thức cho Thống Kê ---
    public List<DailyRevenueDTO> getDailyRevenue(Date startDate, Date endDate) throws DatabaseOperationException {
        List<DailyRevenueDTO> dailyRevenues = new ArrayList<>();
        String sql = "SELECT DATE(o.created_at) AS order_day, SUM(o.quantity * i.item_price) AS daily_total "
                + "FROM orders o "
                + "JOIN items i ON o.item_id = i.item_id "
                + "WHERE DATE(o.created_at) BETWEEN ? AND ? "
                + "GROUP BY DATE(o.created_at) "
                + "ORDER BY order_day ASC";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, startDate);
            pstmt.setDate(2, endDate);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                dailyRevenues.add(new DailyRevenueDTO(rs.getDate("order_day"), rs.getDouble("daily_total")));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi lấy doanh thu theo ngày: " + e.getMessage(), e);
        }
        return dailyRevenues;
    }

    public List<TopSellingItemDTO> getTopSellingItems(int limit, Date startDate, Date endDate) throws DatabaseOperationException {
        List<TopSellingItemDTO> topItems = new ArrayList<>();
        String sql = "SELECT i.item_name, SUM(o.quantity) AS total_sold "
                + "FROM orders o "
                + "JOIN items i ON o.item_id = i.item_id "
                + "WHERE DATE(o.created_at) BETWEEN ? AND ? "
                + "GROUP BY i.item_name "
                + "ORDER BY total_sold DESC "
                + "LIMIT ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, startDate);
            pstmt.setDate(2, endDate);
            pstmt.setInt(3, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                topItems.add(new TopSellingItemDTO(rs.getString("item_name"), rs.getInt("total_sold")));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi lấy top sản phẩm bán chạy: " + e.getMessage(), e);
        }
        return topItems;
    }

    public static void main(String[] args) {
        /* ... (main test nếu có) ... */ }
}
