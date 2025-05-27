package com.coffeeshop.core.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.coffeeshop.core.dao.InventoryDAO;
import com.coffeeshop.core.dao.ItemDAO;
import com.coffeeshop.core.dao.OrderDAO;
import com.coffeeshop.core.dao.RecipeDAO;
import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.model.Inventory;
import com.coffeeshop.core.model.OrderLine;
import com.coffeeshop.core.model.RecipeEntry;
import com.coffeeshop.core.util.DatabaseConnector;

public class OrderService {

    private OrderDAO orderDAO;
    private InventoryDAO inventoryDAO;
    private RecipeDAO recipeDAO;
    private ItemDAO itemDAO;

    public OrderService() {
        this.orderDAO = new OrderDAO();
        this.inventoryDAO = new InventoryDAO();
        this.recipeDAO = new RecipeDAO();
        this.itemDAO = new ItemDAO();
    }

    public void placeOrderLine(OrderLine orderLine) throws DatabaseOperationException, IllegalStateException {
        Connection conn = null;
        try {
            conn = DatabaseConnector.getConnection();
            System.out.println("OrderService: Connection obtained for transaction. HashCode: " + conn.hashCode() + ", AutoCommit: " + conn.getAutoCommit());
            conn.setAutoCommit(false);
            System.out.println("OrderService: Transaction started (AutoCommit set to false).");

            String itemSku = itemDAO.findById(orderLine.getItemId(), conn) // Sử dụng conn
                    .orElseThrow(() -> new IllegalStateException("Item ID " + orderLine.getItemId() + " không tồn tại trong cơ sở dữ liệu."))
                    .getSku();
            System.out.println("OrderService: Item SKU " + itemSku + " found for Item ID " + orderLine.getItemId());

            if (itemSku == null || itemSku.isEmpty()) {
                // conn.rollback(); // Không cần rollback ở đây nếu chưa có thao tác ghi
                throw new IllegalStateException("Item ID " + orderLine.getItemId() + " không có SKU hợp lệ.");
            }

            List<RecipeEntry> recipeEntries = recipeDAO.getIngredientsForRecipe(itemSku, conn); // Sử dụng conn
            System.out.println("OrderService: Found " + recipeEntries.size() + " recipe entries for SKU " + itemSku);

            if (recipeEntries.isEmpty()) {
                System.out.println("Thông báo: Item " + orderLine.getItemId() + " (SKU: " + itemSku + ") không có công thức. Không trừ kho.");
            } else {
                System.out.println("OrderService: Processing inventory updates for " + recipeEntries.size() + " ingredients.");
                for (RecipeEntry recipeComponent : recipeEntries) {
                    String ingId = recipeComponent.getIngId();
                    int quantityNeededPerItem = recipeComponent.getQuantity();
                    int totalQuantityNeeded = quantityNeededPerItem * orderLine.getQuantity();
                    System.out.println("  OrderService: Ingredient " + ingId + ", needed: " + totalQuantityNeeded);

                    if (totalQuantityNeeded <= 0) {
                        conn.rollback();
                        System.err.println("OrderService: Rollback due to invalid quantity needed for " + ingId);
                        throw new IllegalStateException("Số lượng nguyên liệu cần thiết không hợp lệ cho " + ingId);
                    }

                    Optional<Inventory> currentInventoryOpt = inventoryDAO.findByIngId(ingId, conn); // Sử dụng conn
                    int currentQuantityInStock = currentInventoryOpt.map(Inventory::getQuantity).orElse(0);
                    System.out.println("  OrderService: Ingredient " + ingId + ", current stock: " + currentQuantityInStock);

                    if (currentQuantityInStock < totalQuantityNeeded) {
                        conn.rollback();
                        System.err.println("OrderService: Rollback due to insufficient stock for " + ingId);
                        throw new IllegalStateException("Không đủ tồn kho cho nguyên liệu ID: " + ingId
                                + " (cần " + totalQuantityNeeded + ", có " + currentQuantityInStock + ")");
                    }

                    boolean success = inventoryDAO.upsertInventoryQuantity(ingId, -totalQuantityNeeded, conn); // Sử dụng conn
                    if (!success) {
                        conn.rollback();
                        System.err.println("OrderService: Rollback due to failed inventory update for " + ingId);
                        throw new DatabaseOperationException("Không thể cập nhật tồn kho cho ingredient ID: " + ingId + ". Upsert thất bại.");
                    }
                    System.out.println("  OrderService: Inventory updated for " + ingId + ". Success: " + success);
                }
            }

            orderDAO.createOrderLine(orderLine, conn); // Sử dụng conn
            System.out.println("OrderService: OrderLine created in DB.");

            conn.commit();
            System.out.println("OrderService: Transaction committed successfully for order item " + orderLine.getItemId());

        } catch (SQLException e) {
            System.err.println("OrderService: SQLException during transaction! Details: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("OrderService: Transaction rolled back due to SQLException.");
                } catch (SQLException exRollback) {
                    System.err.println("Lỗi nghiêm trọng khi rollback transaction sau SQLException: " + exRollback.getMessage());
                    exRollback.printStackTrace(); // In stack trace của lỗi rollback
                }
            }
            throw new DatabaseOperationException("Lỗi xử lý đặt hàng (SQL transaction): " + e.getMessage(), e);
        } catch (DatabaseOperationException | IllegalStateException e) {
            System.err.println("OrderService: Business/DB Operation Exception! Details: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    // Chỉ rollback nếu transaction đang hoạt động (chưa commit hoặc rollback)
                    if (!conn.getAutoCommit()) { // Kiểm tra xem autoCommit có phải là false không
                        conn.rollback();
                        System.err.println("OrderService: Transaction rolled back due to: " + e.getClass().getSimpleName());
                    } else {
                        System.err.println("OrderService: AutoCommit was true, no explicit rollback needed for B/DB Exception, but transaction might be inconsistent.");
                    }
                } catch (SQLException exRollback) {
                    System.err.println("Lỗi nghiêm trọng khi rollback transaction sau Business/DB Exception: " + exRollback.getMessage());
                    exRollback.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    if (!conn.isClosed()) {
                        conn.setAutoCommit(true);
                        System.out.println("OrderService: Auto-commit reset for connection. HashCode: " + conn.hashCode());
                    }
                } catch (SQLException e) {
                    System.err.println("Lỗi khi reset auto-commit: " + e.getMessage());
                    e.printStackTrace();
                }
                // KHÔNG đóng connection ở đây: DatabaseConnector.closeConnection();
            }
        }
    }
}
