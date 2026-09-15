package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// 商品上傳附件：檔案「內容本身」直接存進資料庫（file_data 這個 BLOB 欄位），
// 不再寫進本機硬碟——避免部署到雲端（例如 Render 免費方案）時，
// 應用程式重啟導致本機檔案系統被清空，造成附件憑空消失的問題。
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

    // 檔案的實際位元組內容，直接存進資料庫（MySQL 對應 LONGBLOB 欄位）。
    // 這個欄位取代了原本的 storedFileName / filePath——不再需要幫檔案取一個
    // 硬碟上的儲存檔名，也不需要記錄硬碟路徑，因為根本沒有實體檔案存在。
    // 明確指定 columnDefinition = LONGBLOB，不要依賴 Hibernate 自動判斷欄位型別——
    // 不同版本的 Hibernate 對 @Lob + byte[] 在 MySQL 上判斷出來的欄位大小不一定一致
    // （曾經有版本會判斷成 TINYBLOB，上限只有 255 bytes，檔案一大就直接存不進去）。
    @Lob
    @Column(name = "file_data", columnDefinition = "LONGBLOB")
    private byte[] fileData;

    @Column(nullable = false)
    private Long fileSize;

    // 上傳當下的 MIME type（例如 image/png、application/pdf），讓瀏覽器知道怎麼呈現這個檔案
    @Column(length = 100)
    private String contentType;

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

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
