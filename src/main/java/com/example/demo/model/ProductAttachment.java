package com.example.demo.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

    // 檔案的實際位元組內容，直接存進資料庫。
    // 這個欄位取代了原本的 storedFileName / filePath——不再需要幫檔案取一個
    // 硬碟上的儲存檔名，也不需要記錄硬碟路徑，因為根本沒有實體檔案存在。
    //
    // 型別選用踩過兩次坑，記錄一下：
    // 1) 原本寫死 columnDefinition = "LONGBLOB"（MySQL 專屬型別名稱），PostgreSQL 不認得。
    // 2) 改成 @JdbcTypeCode(SqlTypes.BLOB) 後，PostgreSQL 建表變成走舊式的 large object
    //    （欄位型別是 oid，只存一個指標），但實際寫入資料時 JDBC 驅動是用 bytea 的方式
    //    綁定參數，兩邊對不起來，會噴 "column is of type oid but expression is of type bytea"。
    // 最後改用 @JdbcTypeCode(SqlTypes.LONGVARBINARY)，兩個資料庫實測建表結果如下：
    //   MySQL 8      -> longblob（容量上限約 4GB）
    //   PostgreSQL 16 -> bytea（新式做法，欄位直接存內容本身，不用 large object API）
    // 兩邊的「欄位型別」跟「寫入方式」一致，不會再有型別不符的問題。
    //
    // length = 20971520（20MB）要跟 application.properties 的 spring.servlet.multipart.max-file-size
    // 保持一致。這個長度超過 MySQL MEDIUMBLOB 的 16MB 上限，Hibernate 才會選 longblob；
    // 如果拿掉 length，預設長度會讓 MySQL 建出容量小很多的欄位，大檔案就存不進去。
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "file_data", length = 20971520)
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
