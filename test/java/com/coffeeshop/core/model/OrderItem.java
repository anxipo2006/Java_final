package com.coffeeshop.core.model;

import java.math.BigDecimal;

public class OrderItem {

    private int orderItemId; // Khóa chính tự tăng của bảng order_items
    private String orderId;    // Khóa ngoại tới Order
    private String itemId;     // Khóa ngoại tới MenuItem
    private int quantity;
    private BigDecimal itemPriceAtOrder; // Giá của mặt hàng tại thời điểm đặt hàng

    // Optional:
    // private MenuItem menuItemDetails;
    // Constructors
    public OrderItem() {
    }

    public OrderItem(String orderId, String itemId, int quantity, BigDecimal itemPriceAtOrder) {
        this.orderId = orderId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.itemPriceAtOrder = itemPriceAtOrder;
    }

    // Getters and Setters
    public int getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(int orderItemId) {
        this.orderItemId = orderItemId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
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

    public BigDecimal getItemPriceAtOrder() {
        return itemPriceAtOrder;
    }

    public void setItemPriceAtOrder(BigDecimal itemPriceAtOrder) {
        this.itemPriceAtOrder = itemPriceAtOrder;
    }

    // // Optional
    // public MenuItem getMenuItemDetails() {
    //     return menuItemDetails;
    // }
    // public void setMenuItemDetails(MenuItem menuItemDetails) {
    //     this.menuItemDetails = menuItemDetails;
    // }
    // Tính thành tiền cho mục này
    public BigDecimal getSubtotal() {
        if (itemPriceAtOrder == null || quantity <= 0) {
            return BigDecimal.ZERO;
        }
        return itemPriceAtOrder.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public String toString() {
        return "OrderItem{"
                + "orderItemId=" + orderItemId
                + ", orderId='" + orderId + '\''
                + ", itemId='" + itemId + '\''
                + ", quantity=" + quantity
                + ", itemPriceAtOrder=" + itemPriceAtOrder
                + ", subtotal=" + getSubtotal()
                + '}';
    }
}
