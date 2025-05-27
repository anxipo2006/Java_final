package com.coffeeshop.core.model.dto;

public class TopSellingItemDTO {

    private String itemName;
    private int totalQuantitySold;

    public TopSellingItemDTO(String itemName, int totalQuantitySold) {
        this.itemName = itemName;
        this.totalQuantitySold = totalQuantitySold;
    }

    public String getItemName() {
        return itemName;
    }

    public int getTotalQuantitySold() {
        return totalQuantitySold;
    }
}
