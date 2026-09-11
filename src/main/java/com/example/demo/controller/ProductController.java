package com.example.demo.controller;

import com.example.demo.model.PageResponse;
import com.example.demo.model.Product;
import com.example.demo.service.ProductService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // 換頁 + 查詢: /api/products?page=1&size=10&keyword=滑鼠
    // keyword 不帶或帶空字串，就是不篩選，回傳全部商品的分頁結果
    @GetMapping
    public PageResponse<Product> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return productService.findPage(page, size, keyword);
    }

    @GetMapping("/{id}")
    public Product getOne(@PathVariable Long id) {
        return productService.findById(id);
    }
}
