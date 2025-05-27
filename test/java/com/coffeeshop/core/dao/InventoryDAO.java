package com.coffeeshop.core.dao;

import java.sql.Connection;
import java.sql.PreparedStatement; // Cần cho main test
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.model.Inventory;
import com.coffeeshop.core.util.DatabaseConnector;

public class InventoryDAO {

    // Lớp nội tĩnh DTO
    public static class InventoryView {

        public String invId;
        public String ingId;
        public String ingredientName;
        public int quantity;
        public String ingMeas;

        public InventoryView(String invId, String ingId, String ingredientName, int quantity, String ingMeas) {
            this.invId = invId;
            this.ingId = ingId;
            this.ingredientName = ingredientName;
            this.quantity = quantity;
            this.ingMeas = ingMeas;
        }
    }

    private Inventory mapRowToInventory(ResultSet rs) throws SQLException {
        Inventory inventory = new Inventory();
        inventory.setInvId(rs.getString("inv_id"));
        inventory.setIngId(rs.getString("ing_id"));
        inventory.setQuantity(rs.getInt("quantity"));
        return inventory;
    }

    private InventoryView mapRowToInventoryView(ResultSet rs) throws SQLException {
        return new InventoryView(
                rs.getString("inv_id"),
                rs.getString("ing_id"),
                rs.getString("ing_name"), // Từ bảng ingredients
                rs.getInt("quantity"), // Từ bảng inventory
                rs.getString("ing_meas") // Từ bảng ingredients
        );
    }

    public Inventory createInventoryEntry(Inventory inventory) throws DatabaseOperationException {
        if (inventory.getInvId() == null || inventory.getInvId().isEmpty()) {
            throw new DatabaseOperationException("Inventory ID không được để trống khi tạo mới.");
        }
        IngredientDAO ingredientDAO = new IngredientDAO();
        if (!ingredientDAO.findById(inventory.getIngId()).isPresent()) {
            throw new DatabaseOperationException("Không thể tạo inventory: Ingredient ID '" + inventory.getIngId() + "' không tồn tại.");
        }

        String sql = "INSERT INTO inventory (inv_id, ing_id, quantity) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, inventory.getInvId());
            pstmt.setString(2, inventory.getIngId());
            pstmt.setInt(3, inventory.getQuantity());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseOperationException("Tạo inventory entry thất bại.");
            }
            return inventory;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new DatabaseOperationException("Tạo inventory thất bại: Inv_ID '" + inventory.getInvId() + "' hoặc Ing_ID '" + inventory.getIngId() + "' đã tồn tại.", e);
            }
            throw new DatabaseOperationException("Lỗi tạo inventory entry: " + e.getMessage(), e);
        }
    }

    // Phương thức này được gọi bởi SimpleInventoryUI để điền vào JTable
    public List<InventoryView> getAllInventoryWithIngredientNames() throws DatabaseOperationException {
        List<InventoryView> inventoryViews = new ArrayList<>();
        String sql = "SELECT inv.inv_id, inv.ing_id, i.ing_name, inv.quantity, i.ing_meas "
                + "FROM inventory inv "
                + "JOIN ingredients i ON inv.ing_id = i.ing_id "
                + "ORDER BY i.ing_name ASC"; // Sắp xếp cho dễ nhìn
        try (Connection conn = DatabaseConnector.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                inventoryViews.add(mapRowToInventoryView(rs));
            }
        } catch (SQLException e) {
            System.err.println("[InventoryDAO.getAllWithNames] SQLException: " + e.getMessage());
            e.printStackTrace();
            throw new DatabaseOperationException("Lỗi lấy danh sách tồn kho với tên nguyên liệu", e);
        }
        return inventoryViews;
    }

    // --- FIND BY INGREDIENT ID ---
    // Phiên bản public, tự quản lý connection
    public Optional<Inventory> findByIngId(String ingId) throws DatabaseOperationException {
        try (Connection conn = DatabaseConnector.getConnection()) {
            return findByIngId(ingId, conn);
        } catch (SQLException e) {
            System.err.println("[InventoryDAO.findByIngId public] SQLException on getConnection: " + e.getMessage());
            throw new DatabaseOperationException("Lỗi khi lấy connection cho InventoryDAO.findByIngId: " + ingId, e);
        }
    }

    // Phiên bản nhận Connection (quan trọng cho transaction từ OrderService)
    public Optional<Inventory> findByIngId(String ingId, Connection conn) throws DatabaseOperationException {
        System.out.println("[InventoryDAO.findByIngId(conn)] Attempting for ingId: " + ingId);
        String sql = "SELECT inv_id, ing_id, quantity FROM inventory WHERE ing_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ingId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                System.out.println("[InventoryDAO.findByIngId(conn)] Found for ingId: " + ingId);
                return Optional.of(mapRowToInventory(rs));
            }
            System.out.println("[InventoryDAO.findByIngId(conn)] NOT Found for ingId: " + ingId);
        } catch (SQLException e) {
            System.err.println("[InventoryDAO.findByIngId(conn)] SQLException for ingId " + ingId + ": " + e.getMessage());
            throw new DatabaseOperationException("Lỗi tìm inventory theo Ingredient ID (với conn): " + ingId, e);
        }
        return Optional.empty();
    }

    // --- UPSERT INVENTORY QUANTITY ---
    // Phiên bản public, tự quản lý transaction CỤC BỘ.
    public boolean upsertInventoryQuantity(String ingId, int quantityChange) throws DatabaseOperationException {
        Connection conn = null;
        boolean originalAutoCommitState = true;
        System.out.println("[InventoryDAO.upsert public] For ingId: " + ingId + ", quantityChange: " + quantityChange);
        try {
            conn = DatabaseConnector.getConnection();
            originalAutoCommitState = conn.getAutoCommit();
            conn.setAutoCommit(false);

            boolean success = upsertInventoryQuantityInternal(ingId, quantityChange, conn);

            if (success) {
                conn.commit();
                System.out.println("[InventoryDAO.upsert public] Transaction committed for ingId: " + ingId);
            } else {
                System.err.println("[InventoryDAO.upsert public] Internal upsert returned false, rolling back for ingId: " + ingId);
                conn.rollback();
            }
            return success;
        } catch (SQLException | DatabaseOperationException e) { // Bắt cả SQLException và DatabaseOperationException từ internal
            System.err.println("[InventoryDAO.upsert public] Exception for ingId " + ingId + ". Rolling back. Message: " + e.getMessage());
            // e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[InventoryDAO.upsert public] CRITICAL: Lỗi rollback trong upsert (public): " + ex.getMessage());
                }
            }
            // Gói lại nếu là SQLException, ném lại nếu là DatabaseOperationException
            if (e instanceof SQLException) {
                throw new DatabaseOperationException("Lỗi SQL trong upsertInventoryQuantity (public) cho ing_id: " + ingId, e);
            } else {
                throw (DatabaseOperationException) e;
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(originalAutoCommitState);
                } catch (SQLException e) {
                    System.err.println("[InventoryDAO.upsert public] Lỗi reset auto-commit: " + e.getMessage());
                }
            }
        }
    }

    // Phiên bản nhận Connection (được gọi bởi OrderService)
    // KHÔNG quản lý transaction, ném Exception lên cho Service xử lý.
    public boolean upsertInventoryQuantity(String ingId, int quantityChange, Connection conn)
            throws DatabaseOperationException, SQLException {
        return upsertInventoryQuantityInternal(ingId, quantityChange, conn);
    }

    // Hàm private chứa logic upsert cốt lõi
    private boolean upsertInventoryQuantityInternal(String ingId, int quantityChange, Connection conn)
            throws DatabaseOperationException, SQLException {
        Optional<Inventory> existingInventoryOpt = findByIngId(ingId, conn); // Sử dụng conn được truyền vào

        if (existingInventoryOpt.isPresent()) {
            Inventory inv = existingInventoryOpt.get();
            int newQuantity = inv.getQuantity() + quantityChange;
            System.out.println("  [InventoryDAO.upsertInternal] Updating. IngID: " + ingId + ", Old Qty: " + inv.getQuantity() + ", Change: " + quantityChange + ", New Qty: " + newQuantity);

            String sqlUpdate = "UPDATE inventory SET quantity = ? WHERE ing_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                pstmt.setInt(1, newQuantity);
                pstmt.setString(2, ingId);
                int affectedRows = pstmt.executeUpdate();
                System.out.println("  [InventoryDAO.upsertInternal] Update for " + ingId + " affected rows: " + affectedRows);
                return affectedRows > 0;
            } // SQLException sẽ được ném lên nếu có lỗi
        } else {
            if (quantityChange < 0) {
                System.err.println("  [InventoryDAO.upsertInternal] Cannot insert new inventory for " + ingId + " with negative quantity " + quantityChange);
                throw new DatabaseOperationException("Không thể giảm số lượng (" + quantityChange + ") cho ing_id: " + ingId + " vì nó chưa tồn tại trong kho.");
            }
            String newInvId = "INV_AUTO_" + ingId.replace("-", "") + System.currentTimeMillis() % 10000;
            System.out.println("  [InventoryDAO.upsertInternal] Inserting. IngID: " + ingId + ", New InvID: " + newInvId + ", Qty: " + quantityChange);

            String sqlInsert = "INSERT INTO inventory (inv_id, ing_id, quantity) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                pstmt.setString(1, newInvId);
                pstmt.setString(2, ingId);
                pstmt.setInt(3, quantityChange);
                int affectedRows = pstmt.executeUpdate();
                System.out.println("  [InventoryDAO.upsertInternal] Insert for " + ingId + " affected rows: " + affectedRows);
                return affectedRows > 0;
            } catch (SQLException e) {
                if (e.getErrorCode() == 1062) {
                    throw new DatabaseOperationException("Lỗi thêm mới inventory (trùng khóa): Inv_ID '" + newInvId + "' hoặc Ing_ID '" + ingId + "' đã tồn tại.", e);
                }
                throw e; // Ném lại SQLException
            }
        }
    }

    // Main method để test
    public static void main(String[] args) {
        InventoryDAO dao = new InventoryDAO();
        IngredientDAO ingredientDAO = new IngredientDAO();
        try {
            // Test getAllInventoryWithIngredientNames
            System.out.println("--- Tất cả Inventory Với Tên Nguyên Liệu ---");
            List<InventoryView> views = dao.getAllInventoryWithIngredientNames();
            if (views.isEmpty()) {
                System.out.println("Không có dữ liệu tồn kho.");
            } else {
                views.forEach(v -> System.out.println(
                        String.format("InvID: %s, IngID: %s, Tên: %s, SL: %d %s",
                                v.invId, v.ingId, v.ingredientName, v.quantity, v.ingMeas)
                ));
            }
            // Test upsert
            String testIngId = "ING001"; // Espresso Beans
            if (ingredientDAO.findById(testIngId).isPresent()) {
                System.out.println("\n--- Test Upsert Inventory cho ING001 ---");
                System.out.println("Trạng thái ban đầu của ING001:");
                dao.findByIngId(testIngId).ifPresentOrElse(
                        inv -> System.out.println("  " + inv),
                        () -> System.out.println("  ING001 chưa có trong kho.")
                );

                System.out.println("Thử thêm 5000 vào ING001");
                dao.upsertInventoryQuantity(testIngId, 5000);
                dao.findByIngId(testIngId).ifPresent(inv -> System.out.println("  Sau khi thêm: " + inv));

                System.out.println("Thử bớt 100 từ ING001");
                dao.upsertInventoryQuantity(testIngId, -100);
                dao.findByIngId(testIngId).ifPresent(inv -> System.out.println("  Sau khi bớt: " + inv));

            } else {
                System.out.println("Không thể test upsert, ING001 không tồn tại trong ingredients.");
            }

        } catch (DatabaseOperationException e) {
            e.printStackTrace();
        } finally {
            // DatabaseConnector.closeConnection(); // Xem xét việc đóng connection ở đây
        }
    }
}
