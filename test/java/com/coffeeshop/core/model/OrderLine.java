package com.coffeeshop.core.model;

import java.sql.Timestamp; // Hoặc java.time.LocalDateTime

public class OrderLine {

    private int rowId;        // row_id
    private String orderId;   // order_id
    private Timestamp createdAt; // created_at
    private String itemId;    // item_id (FK to items)
    private int quantity;
    private String custName;
    private String inOrOut;

    // Constructors
    public OrderLine() {
    }

    public OrderLine(String orderId, Timestamp createdAt, String itemId, int quantity, String custName, String inOrOut) {
        this.orderId = orderId;
        this.createdAt = createdAt;
        this.itemId = itemId;
        this.quantity = quantity;
        this.custName = custName;
        this.inOrOut = inOrOut;
    }

    public OrderLine(int rowId, String orderId, Timestamp createdAt, String itemId, int quantity, String custName, String inOrOut) {
        this.rowId = rowId;
        this.orderId = orderId;
        this.createdAt = createdAt;
        this.itemId = itemId;
        this.quantity = quantity;
        this.custName = custName;
        this.inOrOut = inOrOut;
    }

    // Getters and Setters
    public int getRowId() {
        return rowId;
    }

    public void setRowId(int rowId) {
        this.rowId = rowId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getCustName() {
        return custName;
    }

    public void setCustName(String custName) {
        this.custName = custName;
    }

    public String getInOrOut() {
        return inOrOut;
    }

    public void setInOrOut(String inOrOut) {
        this.inOrOut = inOrOut;
    }

    @Override
    public String toString() {
        return "OrderLine{"
                + "rowId=" + rowId
                + ", orderId='" + orderId + '\''
                + ", createdAt=" + createdAt
                + ", itemId='" + itemId + '\''
                + ", quantity=" + quantity
                + ", custName='" + custName + '\''
                + ", inOrOut='" + inOrOut + '\''
                + '}';
    }
}
