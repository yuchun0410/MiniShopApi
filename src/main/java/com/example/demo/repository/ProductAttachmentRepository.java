package com.example.demo.repository;

import com.example.demo.model.ProductAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductAttachmentRepository extends JpaRepository<ProductAttachment, Long> {
    List<ProductAttachment> findByProductId(Long productId);
    void deleteByProductId(Long productId);
}
