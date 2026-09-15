package com.example.demo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique = true：商品名稱不能重複，Hibernate 會在這個欄位上建一個 UNIQUE 索引
    // 注意：如果資料庫裡目前已經有重複名稱的資料，ddl-auto=update 加這個限制可能會失敗或被跳過，
    // 要先手動清過資料再重啟
    @Column(nullable = false, length = 100, unique = true)
    private String name;

    // 金額用 BigDecimal,不要用 double/float(浮點數運算會有誤差問題)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    // ---- getters / setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}