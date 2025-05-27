package com.coffeeshop.core.model;

public class Item {

    private String itemId;
    private String sku;
    private String itemName;
    private String itemCat; // item_cat
    private String itemSize;
    private double itemPrice; // item_price

    // Constructors
    public Item() {
    }

    public Item(String itemId, String sku, String itemName, String itemCat, String itemSize, double itemPrice) {
        this.itemId = itemId;
        this.sku = sku;
        this.itemName = itemName;
        this.itemCat = itemCat;
        this.itemSize = itemSize;
        this.itemPrice = itemPrice;
    }

    // Getters and Setters
    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemCat() {
        return itemCat;
    }

    public void setItemCat(String itemCat) {
        this.itemCat = itemCat;
    }

    public String getItemSize() {
        return itemSize;
    }

    public void setItemSize(String itemSize) {
        this.itemSize = itemSize;
    }

    public double getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(double itemPrice) {
        this.itemPrice = itemPrice;
    }

    // toString (hữu ích cho việc debug)
    @Override
    public String toString() {
        return "Item{"
                + "itemId='" + itemId + '\''
                + ", sku='" + sku + '\''
                + ", itemName='" + itemName + '\''
                + ", itemCat='" + itemCat + '\''
                + ", itemSize='" + itemSize + '\''
                + ", itemPrice=" + itemPrice
                + '}';
    }
}
