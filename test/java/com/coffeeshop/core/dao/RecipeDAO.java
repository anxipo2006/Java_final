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
import com.coffeeshop.core.model.RecipeEntry;
import com.coffeeshop.core.util.DatabaseConnector;

public class RecipeDAO {

    // --- DTO RecipeEntryView ---
    public static class RecipeEntryView {

        public int rowId;
        public String recipeId;
        public String itemName;
        public String ingId;
        public String ingredientName;
        public int quantity;
        public String ingMeas;

        public RecipeEntryView(int rowId, String recipeId, String itemName, String ingId, String ingredientName, int quantity, String ingMeas) {
            this.rowId = rowId;
            this.recipeId = recipeId;
            this.itemName = itemName;
            this.ingId = ingId;
            this.ingredientName = ingredientName;
            this.quantity = quantity;
            this.ingMeas = ingMeas;
        }
    }

    private RecipeEntry mapRowToRecipeEntry(ResultSet rs) throws SQLException {
        RecipeEntry entry = new RecipeEntry();
        entry.setRowId(rs.getInt("row_id"));
        entry.setRecipeId(rs.getString("recipe_id"));
        entry.setIngId(rs.getString("ing_id"));
        entry.setQuantity(rs.getInt("quantity"));
        return entry;
    }

    private RecipeEntryView mapRowToRecipeEntryView(ResultSet rs) throws SQLException {
        return new RecipeEntryView(
                rs.getInt("r_row_id"), // Sử dụng alias nếu có trong câu SQL JOIN
                rs.getString("recipe_id"),
                rs.getString("item_name"),
                rs.getString("ing_id"),
                rs.getString("ing_name"),
                rs.getInt("quantity"),
                rs.getString("ing_meas")
        );
    }

    // --- Phiên bản được OrderService sử dụng, nhận Connection ---
    public List<RecipeEntry> getIngredientsForRecipe(String recipeId_SKU, Connection conn) throws DatabaseOperationException {
        List<RecipeEntry> entries = new ArrayList<>();
        String sql = "SELECT row_id, recipe_id, ing_id, quantity FROM recipe WHERE recipe_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, recipeId_SKU);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                entries.add(mapRowToRecipeEntry(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi lấy ingredients cho recipe_id (với conn): " + recipeId_SKU, e);
        }
        return entries;
    }

    // --- Các phương thức public khác tự quản lý connection ---
    public List<RecipeEntry> getIngredientsForRecipe(String recipeId_SKU) throws DatabaseOperationException {
        try (Connection conn = DatabaseConnector.getConnection()) {
            return getIngredientsForRecipe(recipeId_SKU, conn);
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi khi lấy connection cho RecipeDAO.getIngredientsForRecipe: " + recipeId_SKU, e);
        }
    }

    public RecipeEntry addIngredientToRecipe(RecipeEntry recipeEntry) throws DatabaseOperationException {
        // Kiểm tra FKs
        ItemDAO itemDAO = new ItemDAO(); // Sẽ tự lấy connection
        if (!itemDAO.findBySku(recipeEntry.getRecipeId()).isPresent()) {
            throw new DatabaseOperationException("Không thể thêm vào công thức: Item SKU '" + recipeEntry.getRecipeId() + "' không tồn tại.");
        }
        IngredientDAO ingredientDAO = new IngredientDAO(); // Sẽ tự lấy connection
        if (!ingredientDAO.findById(recipeEntry.getIngId()).isPresent()) {
            throw new DatabaseOperationException("Không thể thêm vào công thức: Ingredient ID '" + recipeEntry.getIngId() + "' không tồn tại.");
        }
        if (getRecipeEntryByRecipeAndIngredient(recipeEntry.getRecipeId(), recipeEntry.getIngId()).isPresent()) {
            throw new DatabaseOperationException("Nguyên liệu '" + recipeEntry.getIngId() + "' đã tồn tại trong công thức cho item '" + recipeEntry.getRecipeId() + "'.");
        }

        String sql = "INSERT INTO recipe (recipe_id, ing_id, quantity) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, recipeEntry.getRecipeId());
            pstmt.setString(2, recipeEntry.getIngId());
            pstmt.setInt(3, recipeEntry.getQuantity());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseOperationException("Thêm thành phần công thức thất bại.");
            }
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    recipeEntry.setRowId(generatedKeys.getInt(1));
                } else {
                    System.out.println("Không lấy được row_id tự tăng cho recipe entry mới.");
                }
            }
            return recipeEntry;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new DatabaseOperationException("Thành phần công thức đã tồn tại (recipe_id, ing_id).", e);
            }
            throw new DatabaseOperationException("Lỗi khi thêm thành phần công thức: " + e.getMessage(), e);
        }
    }

    public Optional<RecipeEntry> getRecipeEntryByRecipeAndIngredient(String recipeId, String ingId) throws DatabaseOperationException {
        String sql = "SELECT row_id, recipe_id, ing_id, quantity FROM recipe WHERE recipe_id = ? AND ing_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, recipeId);
            pstmt.setString(2, ingId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToRecipeEntry(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi tìm recipe entry theo recipe_id và ing_id", e);
        }
        return Optional.empty();
    }

    public Optional<RecipeEntry> getRecipeEntryById(int rowId) throws DatabaseOperationException {
        String sql = "SELECT row_id, recipe_id, ing_id, quantity FROM recipe WHERE row_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, rowId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToRecipeEntry(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi tìm recipe entry theo row_id: " + rowId, e);
        }
        return Optional.empty();
    }

    public List<RecipeEntryView> getRecipeDetails(String recipeId_SKU) throws DatabaseOperationException {
        List<RecipeEntryView> entries = new ArrayList<>();
        String sql = "SELECT r.row_id AS r_row_id, r.recipe_id, it.item_name, r.ing_id, ig.ing_name, r.quantity, ig.ing_meas "
                + "FROM recipe r "
                + "JOIN ingredients ig ON r.ing_id = ig.ing_id "
                + "JOIN items it ON r.recipe_id = it.sku "
                + "WHERE r.recipe_id = ? "
                + "ORDER BY ig.ing_name";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, recipeId_SKU);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                entries.add(mapRowToRecipeEntryView(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi lấy chi tiết công thức cho recipe_id: " + recipeId_SKU, e);
        }
        return entries;
    }

    public List<RecipeEntryView> getAllRecipeViews() throws DatabaseOperationException {
        List<RecipeEntryView> entries = new ArrayList<>();
        String sql = "SELECT r.row_id AS r_row_id, r.recipe_id, it.item_name, r.ing_id, ig.ing_name, r.quantity, ig.ing_meas "
                + "FROM recipe r "
                + "JOIN ingredients ig ON r.ing_id = ig.ing_id "
                + "JOIN items it ON r.recipe_id = it.sku "
                + "ORDER BY r.recipe_id, ig.ing_name";
        try (Connection conn = DatabaseConnector.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                entries.add(mapRowToRecipeEntryView(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi lấy tất cả recipe views", e);
        }
        return entries;
    }

    public boolean updateRecipeEntryQuantity(int rowId, int newQuantity) throws DatabaseOperationException {
        if (newQuantity <= 0) {
            throw new DatabaseOperationException("Số lượng nguyên liệu trong công thức phải lớn hơn 0.");
        }
        String sql = "UPDATE recipe SET quantity = ? WHERE row_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newQuantity);
            pstmt.setInt(2, rowId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi cập nhật số lượng cho recipe entry row_id: " + rowId, e);
        }
    }

    public boolean updateRecipeEntryQuantity(String recipeId, String ingId, int newQuantity) throws DatabaseOperationException {
        if (newQuantity <= 0) {
            /* ... */ }
        String sql = "UPDATE recipe SET quantity = ? WHERE recipe_id = ? AND ing_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            /* ... */ return false;
        } // Placeholder
        catch (SQLException e) {
            throw new DatabaseOperationException("", e);
        } // Placeholder
    }

    public boolean removeIngredientFromRecipe(int rowId) throws DatabaseOperationException {
        String sql = "DELETE FROM recipe WHERE row_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            /* ... */ return false;
        } // Placeholder
        catch (SQLException e) {
            throw new DatabaseOperationException("", e);
        } // Placeholder
    }

    public boolean removeIngredientFromRecipe(String recipeId, String ingId) throws DatabaseOperationException {
        String sql = "DELETE FROM recipe WHERE recipe_id = ? AND ing_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            /* ... */ return false;
        } // Placeholder
        catch (SQLException e) {
            throw new DatabaseOperationException("", e);
        } // Placeholder
    }

    public static void main(String[] args) {
        /* ... (Code test giữ nguyên nếu có) ... */ }
}
