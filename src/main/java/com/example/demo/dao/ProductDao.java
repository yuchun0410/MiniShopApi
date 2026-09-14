package com.example.demo.dao;

import com.example.demo.model.Product;
import java.util.List;
import java.util.Optional;

public interface ProductDao {
    Product save(Product product);
    void deleteById(Long id);
    List<Product> findAll();
    Optional<Product> findById(Long id);

    // page 從 1 開始算；keyword 可為 null 或空字串，代表不篩選
    List<Product> findPage(int page, int size, String keyword);
    long count(String keyword);
}
