package com.example.demo.dao;

import com.example.demo.model.ProductAttachment;
import java.util.List;

public interface ProductAttachmentDao {
    ProductAttachment save(ProductAttachment attachment);
    List<ProductAttachment> findByProductId(Long productId);
    void deleteByProductId(Long productId);
}
