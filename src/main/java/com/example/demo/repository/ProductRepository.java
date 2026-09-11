package com.example.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 換頁 + 查詢：商品名稱包含 keyword 的分頁結果
    Page<Product> findByNameContaining(String keyword, Pageable pageable);

    long countByNameContaining(String keyword);
}
