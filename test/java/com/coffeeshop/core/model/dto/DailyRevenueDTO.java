// Tạo trong com.coffeeshop.core.dto hoặc com.coffeeshop.core.model
package com.coffeeshop.core.model.dto;

import java.sql.Date;

public class DailyRevenueDTO {

    private Date orderDate;
    private double totalRevenue;

    public DailyRevenueDTO(Date orderDate, double totalRevenue) {
        this.orderDate = orderDate;
        this.totalRevenue = totalRevenue;
    }

    public Date getOrderDate() {
        return orderDate;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }
}
