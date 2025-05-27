package com.coffeeshop.core.model;

public class RecipeEntry {

    private int rowId;       // row_id, nếu nó là INT và tự tăng, có thể không cần set khi tạo
    private String recipeId; // recipe_id (thường là SKU của item)
    private String ingId;    // ing_id (FK to ingredients)
    private int quantity;    // quantity của ingredient cho recipe này

    // Constructors
    public RecipeEntry() {
    }

    public RecipeEntry(String recipeId, String ingId, int quantity) {
        this.recipeId = recipeId;
        this.ingId = ingId;
        this.quantity = quantity;
    }

    public RecipeEntry(int rowId, String recipeId, String ingId, int quantity) {
        this.rowId = rowId;
        this.recipeId = recipeId;
        this.ingId = ingId;
        this.quantity = quantity;
    }

    // Getters and Setters
    public int getRowId() {
        return rowId;
    }

    public void setRowId(int rowId) {
        this.rowId = rowId;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getIngId() {
        return ingId;
    }

    public void setIngId(String ingId) {
        this.ingId = ingId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "RecipeEntry{"
                + "rowId=" + rowId
                + ", recipeId='" + recipeId + '\''
                + ", ingId='" + ingId + '\''
                + ", quantity=" + quantity
                + '}';
    }
}
