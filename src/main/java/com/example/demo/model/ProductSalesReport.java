package com.example.demo.model;

import java.math.BigDecimal;

// 不是 Entity，不對應資料表，只是拿來裝「統計查詢」算出來的結果
public class ProductSalesReport {

    private Long productId;
    private String productName;
    private Long totalQuantity;
    private BigDecimal totalRevenue;

    // 這個建構子要跟 JPQL 裡 "new com.example.demo.model.ProductSalesReport(...)" 的參數順序完全對上
    public ProductSalesReport(Long productId, String productName, Long totalQuantity, BigDecimal totalRevenue) {
        this.productId = productId;
        this.productName = productName;
        this.totalQuantity = totalQuantity;
        this.totalRevenue = totalRevenue;
    }

    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Long getTotalQuantity() { return totalQuantity; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
}