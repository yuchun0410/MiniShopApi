package com.example.demo.service;

import com.example.demo.model.PageResponse;
import com.example.demo.model.Product;
import java.util.List;

public interface ProductService {
    List<Product> findAll();
    Product findById(Long id);
    PageResponse<Product> findPage(int page, int size, String keyword);
}
