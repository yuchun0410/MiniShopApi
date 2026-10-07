package com.example.demo.dao.impl;

import com.example.demo.dao.ProductDao;
import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Repository("productDaoJpa")
public class ProductDaoJpaImpl implements ProductDao {

    private final ProductRepository productRepository;

    public ProductDaoJpaImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Override
    public void deleteById(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public boolean decreaseStock(Long id, int quantity) {
        return productRepository.decreaseStock(id, quantity) > 0;
    }

    @Override
    public List<Product> findPage(int page, int size, String keyword) {
        // JpaRepository 內建就有分頁支援（PageRequest 是 0-based，所以要減 1）。
        // 有 keyword 就走 findByNameContaining，沒有就走原本的 findAll(Pageable)。
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        if (StringUtils.hasText(keyword)) {
            return productRepository.findByNameContaining(keyword, pageRequest).getContent();
        }
        return productRepository.findAll(pageRequest).getContent();
    }

    @Override
    public long count(String keyword) {
        if (StringUtils.hasText(keyword)) {
            return productRepository.countByNameContaining(keyword);
        }
        return productRepository.count();
    }
}
