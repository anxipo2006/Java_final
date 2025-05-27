package com.coffeeshop.core.model;

// Không cần java.math.BigDecimal nếu dùng double cho giá
// import java.math.BigDecimal;
public class Ingredient {

    private String ingId;
    private String ingName;
    private int ingWeight;    // Đã là int theo DAO của bạn
    private String ingMeas;
    private double ingPrice;  // Đã là double theo DAO của bạn

    // Constructors
    public Ingredient() {
    }

    public Ingredient(String ingId, String ingName, int ingWeight, String ingMeas, double ingPrice) {
        this.ingId = ingId;
        this.ingName = ingName;
        this.ingWeight = ingWeight;
        this.ingMeas = ingMeas;
        this.ingPrice = ingPrice;
    }

    // Getters and Setters
    public String getIngId() {
        return ingId;
    }

    public void setIngId(String ingId) {
        this.ingId = ingId;
    }

    public String getIngName() {
        return ingName;
    }

    public void setIngName(String ingName) {
        this.ingName = ingName;
    }

    public int getIngWeight() {
        return ingWeight;
    }

    public void setIngWeight(int ingWeight) {
        this.ingWeight = ingWeight;
    }

    public String getIngMeas() {
        return ingMeas;
    }

    public void setIngMeas(String ingMeas) {
        this.ingMeas = ingMeas;
    }

    public double getIngPrice() {
        return ingPrice;
    }

    public void setIngPrice(double ingPrice) {
        this.ingPrice = ingPrice;
    }

    // toString (hữu ích cho việc debug)
    @Override
    public String toString() {
        return "Ingredient{"
                + "ingId='" + ingId + '\''
                + ", ingName='" + ingName + '\''
                + ", ingWeight=" + ingWeight
                + ", ingMeas='" + ingMeas + '\''
                + ", ingPrice=" + ingPrice
                + '}';
    }
}
