package com.example.demo.dao.impl;

import com.example.demo.dao.ProductAttachmentDao;
import com.example.demo.model.ProductAttachment;
import com.example.demo.repository.ProductAttachmentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("productAttachmentDaoJpa")
public class ProductAttachmentDaoJpaImpl implements ProductAttachmentDao {

    private final ProductAttachmentRepository productAttachmentRepository;

    public ProductAttachmentDaoJpaImpl(ProductAttachmentRepository productAttachmentRepository) {
        this.productAttachmentRepository = productAttachmentRepository;
    }

    @Override
    public ProductAttachment save(ProductAttachment attachment) {
        return productAttachmentRepository.save(attachment);
    }

    @Override
    public List<ProductAttachment> findByProductId(Long productId) {
        return productAttachmentRepository.findByProductId(productId);
    }

    @Override
    public void deleteByProductId(Long productId) {
        productAttachmentRepository.deleteByProductId(productId);
    }
}
