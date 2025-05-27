package com.coffeeshop.core.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.model.Item;
import com.coffeeshop.core.util.DatabaseConnector;

public class ItemDAO {

    private Item mapRowToItem(ResultSet rs) throws SQLException {
        Item item = new Item();
        item.setItemId(rs.getString("item_id"));
        item.setSku(rs.getString("sku"));
        item.setItemName(rs.getString("item_name"));
        item.setItemCat(rs.getString("item_cat"));
        item.setItemSize(rs.getString("item_size"));
        item.setItemPrice(rs.getDouble("item_price"));
        return item;
    }

    // --- Phiên bản được OrderService sử dụng, nhận Connection ---
    public Optional<Item> findById(String itemId, Connection conn) throws DatabaseOperationException {
        String sql = "SELECT item_id, sku, item_name, item_cat, item_size, item_price FROM items WHERE item_id = ?"; // Thêm các cột rõ ràng
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToItem(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi tìm item theo ID (với conn): " + itemId, e);
        }
        return Optional.empty();
    }

    // --- Các phương thức public khác tự quản lý connection ---
    public Optional<Item> findById(String itemId) throws DatabaseOperationException {
        try (Connection conn = DatabaseConnector.getConnection()) {
            return findById(itemId, conn);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi khi lấy connection cho ItemDAO.findById: " + itemId, e);
        }
    }

    public Optional<Item> findBySku(String sku, Connection conn) throws DatabaseOperationException {
        String sql = "SELECT item_id, sku, item_name, item_cat, item_size, item_price FROM items WHERE sku = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sku);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToItem(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi tìm item theo SKU (với conn): " + sku, e);
        }
        return Optional.empty();
    }

    public Optional<Item> findBySku(String sku) throws DatabaseOperationException {
        try (Connection conn = DatabaseConnector.getConnection()) {
            return findBySku(sku, conn);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi khi lấy connection cho ItemDAO.findBySku: " + sku, e);
        }
    }

    public Item createItem(Item item) throws DatabaseOperationException {
        if (item.getItemId() == null || item.getItemId().isEmpty()) {
            throw new DatabaseOperationException("Item ID không được để trống khi tạo mới.");
        }
        if (item.getSku() == null || item.getSku().isEmpty()) {
            throw new DatabaseOperationException("SKU không được để trống khi tạo mới.");
        }
        String sql = "INSERT INTO items (item_id, sku, item_name, item_cat, item_size, item_price) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, item.getItemId());
            pstmt.setString(2, item.getSku());
            pstmt.setString(3, item.getItemName());
            pstmt.setString(4, item.getItemCat());
            pstmt.setString(5, item.getItemSize());
            pstmt.setDouble(6, item.getItemPrice());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseOperationException("Tạo item thất bại, không có hàng nào bị ảnh hưởng.");
            }
            return item;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // MySQL error code for duplicate entry
                throw new DatabaseOperationException("Tạo item thất bại: Item ID hoặc SKU '" + (e.getMessage().contains(item.getItemId()) ? item.getItemId() : item.getSku()) + "' đã tồn tại.", e);
            }
            throw new DatabaseOperationException("Lỗi tạo item: " + item.getItemName(), e);
        }
    }

    public List<Item> getAllItems() throws DatabaseOperationException {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT item_id, sku, item_name, item_cat, item_size, item_price FROM items ORDER BY item_name";
        try (Connection conn = DatabaseConnector.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(mapRowToItem(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi lấy tất cả items", e);
        }
        return items;
    }

    public boolean updateItem(Item item) throws DatabaseOperationException {
        String sql = "UPDATE items SET sku = ?, item_name = ?, item_cat = ?, item_size = ?, item_price = ? WHERE item_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, item.getSku());
            pstmt.setString(2, item.getItemName());
            pstmt.setString(3, item.getItemCat());
            pstmt.setString(4, item.getItemSize());
            pstmt.setDouble(5, item.getItemPrice());
            pstmt.setString(6, item.getItemId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 && e.getMessage().toLowerCase().contains("sku")) {
                throw new DatabaseOperationException("Cập nhật item thất bại: SKU '" + item.getSku() + "' đã tồn tại cho item khác.", e);
            }
            throw new DatabaseOperationException("Lỗi cập nhật item: " + item.getItemName(), e);
        }
    }

    public boolean deleteItem(String itemId) throws DatabaseOperationException {
        String sql = "DELETE FROM items WHERE item_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            // Check for foreign key constraint violation (e.g., MySQL error code 1451)
            if (e.getErrorCode() == 1451) {
                throw new DatabaseOperationException("Không thể xóa item ID: " + itemId + ". Item này đang có trong các đơn hàng hoặc công thức.", e);
            }
            throw new DatabaseOperationException("Lỗi xóa item ID: " + itemId, e);
        }
    }

    // Hàm main để test (nếu có)
    public static void main(String[] args) {
        // ... (Code test của bạn)
    }
}
