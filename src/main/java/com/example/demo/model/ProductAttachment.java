package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// 商品上傳附件：檔案本身存在硬碟，這裡只存中繼資料（檔名、路徑、大小）
// 跟 Product 用 productId 關聯（沒有用 @ManyToOne，保持跟其他 model 一樣的簡單風格）
@Entity
@Table(name = "product_attachment")
public class ProductAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    // 使用者上傳當下的原始檔名（給人看的，例如 "商品說明.pdf"）
    @Column(nullable = false, length = 255)
    private String originalFileName;

    // 實際存在硬碟上的檔名（UUID 產生，避免不同使用者上傳同名檔案互相覆蓋）
    @Column(nullable = false, length = 255)
    private String storedFileName;

    // 檔案在硬碟上的完整路徑
    @Column(nullable = false, length = 500)
    private String filePath;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    public void prePersist() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }

    // ---- getters / setters ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getStoredFileName() { return storedFileName; }
    public void setStoredFileName(String storedFileName) { this.storedFileName = storedFileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
