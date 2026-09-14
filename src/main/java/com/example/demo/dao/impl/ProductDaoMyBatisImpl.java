package com.example.demo.dao.impl;

import com.example.demo.dao.ProductDao;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.model.Product;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("productDaoMyBatis")
public class ProductDaoMyBatisImpl implements ProductDao {

    private final ProductMapper productMapper;

    public ProductDaoMyBatisImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public Product save(Product product) {
        productMapper.insert(product);
        return product;
    }

    @Override
    public void deleteById(Long id) {
        productMapper.deleteById(id);
    }

    @Override
    public List<Product> findAll() {
        return productMapper.findAll();
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(productMapper.findById(id));
    }

    @Override
    public List<Product> findPage(int page, int size, String keyword) {
        int offset = (page - 1) * size;
        return productMapper.findPage(offset, size, keyword);
    }

    @Override
    public long count(String keyword) {
        return productMapper.count(keyword);
    }
}
