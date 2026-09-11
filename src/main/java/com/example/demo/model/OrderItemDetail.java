package com.example.demo.model;

import java.math.BigDecimal;

// 不是 Entity，不對應資料表。
// OrderItem 現在只存 productId（外鍵），本身沒有商品名稱，
// 前端顯示訂單明細時需要商品名稱，所以在 Service 層把 OrderItem 跟對應的商品名稱組成這個 DTO 一起回傳。
public class OrderItemDetail {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;

    public OrderItemDetail(Long id, Long productId, String productName, Integer quantity, BigDecimal unitPrice) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}
