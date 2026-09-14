package com.example.demo.mapper;

import com.example.demo.model.ProductAttachment;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface ProductAttachmentMapper {
    int insert(ProductAttachment attachment);
    List<ProductAttachment> findByProductId(@Param("productId") Long productId);
    int deleteByProductId(@Param("productId") Long productId);
}
