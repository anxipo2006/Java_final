package com.coffeeshop.com.coffeeshop.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ingredient {

    private String ingId;
    private String ingName;
    private BigDecimal ingPurchaseUnitSize;
    private String ingPurchaseUnitMeasure;
    private BigDecimal ingPricePerPurchaseUnit;

}
