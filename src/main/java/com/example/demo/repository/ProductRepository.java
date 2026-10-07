package com.example.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 換頁 + 查詢：商品名稱包含 keyword 的分頁結果
    Page<Product> findByNameContaining(String keyword, Pageable pageable);

    long countByNameContaining(String keyword);

    // 原子扣庫存：WHERE 帶 stock >= quantity，庫存不足時更新 0 筆
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.id = :id AND p.stock >= :quantity")
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);
}
