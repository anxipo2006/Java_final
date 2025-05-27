package com.coffeeshop.core.dao;

import com.coffeeshop.core.model.Ingredient;
import com.coffeeshop.core.util.DatabaseConnector;
import com.coffeeshop.core.exception.DatabaseOperationException;
import com.coffeeshop.core.exception.DataNotFoundException; // Tùy chọn nếu bạn dùng getIngredientById

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IngredientDAO {

    private Ingredient mapRowToIngredient(ResultSet rs) throws SQLException {
        Ingredient ingredient = new Ingredient();
        ingredient.setIngId(rs.getString("ing_id"));
        ingredient.setIngName(rs.getString("ing_name"));
        ingredient.setIngWeight(rs.getInt("ing_weight"));
        ingredient.setIngMeas(rs.getString("ing_meas"));
        ingredient.setIngPrice(rs.getDouble("ing_price"));
        return ingredient;
    }

    public Ingredient createIngredient(Ingredient ingredient) throws DatabaseOperationException {
        if (ingredient.getIngId() == null || ingredient.getIngId().isEmpty()) {
            // Có thể tự tạo ID ở đây nếu cần, ví dụ: ingredient.setIngId(java.util.UUID.randomUUID().toString().substring(0,6));
            throw new DatabaseOperationException("Ingredient ID không được để trống khi tạo mới.");
        }
        String sql = "INSERT INTO ingredients (ing_id, ing_name, ing_weight, ing_meas, ing_price) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, ingredient.getIngId());
            pstmt.setString(2, ingredient.getIngName());
            pstmt.setInt(3, ingredient.getIngWeight());
            pstmt.setString(4, ingredient.getIngMeas());
            pstmt.setDouble(5, ingredient.getIngPrice());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseOperationException("Tạo ingredient thất bại, không có hàng nào bị ảnh hưởng.");
            }
            return ingredient;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // Lỗi trùng khóa (ing_id)
                throw new DatabaseOperationException("Tạo ingredient thất bại: Ingredient ID '" + ingredient.getIngId() + "' đã tồn tại.", e);
            }
            throw new DatabaseOperationException("Lỗi tạo ingredient: " + ingredient.getIngName(), e);
        }
    }

    public Optional<Ingredient> findById(String ingId) throws DatabaseOperationException {
        String sql = "SELECT * FROM ingredients WHERE ing_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ingId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToIngredient(rs));
            }
        } catch (SQLException e) {
            System.err.println("[IngredientDAO.findById] SQLException for ingId " + ingId + ": " + e.getMessage());
            // e.printStackTrace();
            throw new DatabaseOperationException("Lỗi tìm ingredient theo ID: " + ingId, e);
        }
        return Optional.empty();
    }

    // Phương thức này được gọi bởi SimpleInventoryUI để điền vào JComboBox
    public List<Ingredient> getAllIngredients() throws DatabaseOperationException {
        List<Ingredient> ingredients = new ArrayList<>();
        String sql = "SELECT * FROM ingredients ORDER BY ing_name ASC"; // Sắp xếp để hiển thị ComboBox đẹp hơn
        try (Connection conn = DatabaseConnector.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ingredients.add(mapRowToIngredient(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi lấy tất cả ingredients", e);
        }
        return ingredients;
    }

    public boolean updateIngredient(Ingredient ingredient) throws DatabaseOperationException {
        String sql = "UPDATE ingredients SET ing_name = ?, ing_weight = ?, ing_meas = ?, ing_price = ? WHERE ing_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, ingredient.getIngName());
            pstmt.setInt(2, ingredient.getIngWeight());
            pstmt.setString(3, ingredient.getIngMeas());
            pstmt.setDouble(4, ingredient.getIngPrice());
            pstmt.setString(5, ingredient.getIngId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Lỗi cập nhật ingredient: " + ingredient.getIngName(), e);
        }
    }

    public boolean deleteIngredient(String ingId) throws DatabaseOperationException {
        // Cần kiểm tra ràng buộc khóa ngoại (ví dụ: ingredient có trong recipe hoặc inventory)
        // Hiện tại, DAO chỉ xóa. Service nên xử lý logic kiểm tra này.
        RecipeDAO recipeDAO = new RecipeDAO(); // Giả định đã có
        if (recipeDAO.isIngredientInUse(ingId)) { // Cần tạo phương thức này trong RecipeDAO
            throw new DatabaseOperationException("Không thể xóa nguyên liệu ID: " + ingId + ". Đang được dùng trong công thức.");
        }
        // Tương tự kiểm tra với InventoryDAO nếu cần (InventoryDAO.findByIngId)

        String sql = "DELETE FROM ingredients WHERE ing_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ingId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) { // Lỗi ràng buộc khóa ngoại
                throw new DatabaseOperationException("Không thể xóa ingredient ID: " + ingId + ". Nó đang được sử dụng bởi dữ liệu khác (công thức, tồn kho,...).", e);
            }
            throw new DatabaseOperationException("Lỗi xóa ingredient ID: " + ingId, e);
        }
    }

    // Main method để test
    public static void main(String[] args) {
        IngredientDAO dao = new IngredientDAO();
        try {
            // Test getAllIngredients
            System.out.println("--- Tất cả Ingredients ---");
            List<Ingredient> allIngredients = dao.getAllIngredients();
            allIngredients.forEach(System.out::println);

            // Test findById
            String testId = "ING001";
            System.out.println("\n--- Ingredient với ID: " + testId + " ---");
            dao.findById(testId).ifPresentOrElse(
                    System.out::println,
                    () -> System.out.println("Không tìm thấy ingredient với ID: " + testId)
            );

        } catch (DatabaseOperationException e) {
            e.printStackTrace();
        } finally {
            // DatabaseConnector.closeConnection(); // Không đóng ở đây nếu chạy test nhiều DAO
        }
    }
}
