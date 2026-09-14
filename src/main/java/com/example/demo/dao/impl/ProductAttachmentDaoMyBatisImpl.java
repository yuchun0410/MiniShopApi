package com.example.demo.dao.impl;

import com.example.demo.dao.ProductAttachmentDao;
import com.example.demo.mapper.ProductAttachmentMapper;
import com.example.demo.model.ProductAttachment;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository("productAttachmentDaoMyBatis")
public class ProductAttachmentDaoMyBatisImpl implements ProductAttachmentDao {

    private final ProductAttachmentMapper productAttachmentMapper;

    public ProductAttachmentDaoMyBatisImpl(ProductAttachmentMapper productAttachmentMapper) {
        this.productAttachmentMapper = productAttachmentMapper;
    }

    @Override
    public ProductAttachment save(ProductAttachment attachment) {
        if (attachment.getUploadedAt() == null) {
            attachment.setUploadedAt(LocalDateTime.now());
        }
        productAttachmentMapper.insert(attachment);
        return attachment;
    }

    @Override
    public List<ProductAttachment> findByProductId(Long productId) {
        return productAttachmentMapper.findByProductId(productId);
    }

    @Override
    public void deleteByProductId(Long productId) {
        productAttachmentMapper.deleteByProductId(productId);
    }
}
