package com.example.demo.service.impl;

import com.example.demo.dao.ProductDao;
import com.example.demo.model.PageResponse;
import com.example.demo.model.Product;
import com.example.demo.service.ProductService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductDao productDao;

    // ProductDao 現在有兩個實作（productDaoMyBatis / productDaoJpa），
    // 用 @Qualifier 明確指定要注入哪一個，避免 Spring 因為有多個候選 bean而丟出 NoUniqueBeanDefinitionException。
    // 目前指定用 MyBatis 版，維持原本的行為不變；要切換成 JPA 版，把下面字串改成 "productDaoJpa" 即可。
    public ProductServiceImpl(@Qualifier("productDaoMyBatis") ProductDao productDao) {
        this.productDao = productDao;
    }

    @Override
    public List<Product> findAll() {
        return productDao.findAll();
    }

    @Override
    public Product findById(Long id) {
        return productDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("找不到此商品"));
    }

    @Override
    public PageResponse<Product> findPage(int page, int size, String keyword) {
        List<Product> content = productDao.findPage(page, size, keyword);
        long totalElements = productDao.count(keyword);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
