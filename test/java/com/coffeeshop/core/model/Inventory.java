package com.coffeeshop.core.model;

public class Inventory {

    private String invId;    // inv_id
    private String ingId;    // ing_id (FK to ingredients)
    private int quantity;

    // Constructors
    public Inventory() {
    }

    public Inventory(String invId, String ingId, int quantity) {
        this.invId = invId;
        this.ingId = ingId;
        this.quantity = quantity;
    }

    // Getters and Setters
    public String getInvId() {
        return invId;
    }

    public void setInvId(String invId) {
        this.invId = invId;
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

    // toString
    @Override
    public String toString() {
        return "Inventory{"
                + "invId='" + invId + '\''
                + ", ingId='" + ingId + '\''
                + ", quantity=" + quantity
                + '}';
    }
}
