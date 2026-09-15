package com.example.demo.service.impl;

import com.example.demo.dao.ProductAttachmentDao;
import com.example.demo.dao.ProductDao;
import com.example.demo.model.PageResponse;
import com.example.demo.model.Product;
import com.example.demo.model.ProductAttachment;
import com.example.demo.service.ProductService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductDao productDao;
    private final ProductAttachmentDao productAttachmentDao;

    // ProductDao / ProductAttachmentDao 都各自有兩個實作（MyBatis / JPA），
    // 用 @Qualifier 明確指定要注入哪一個，避免 Spring 因為有多個候選 bean而丟出 NoUniqueBeanDefinitionException。
    // 目前都指定用 MyBatis 版，維持跟其他模組一致；要切換成 JPA 版，把下面字串改成對應的 "...Jpa" 即可。
    public ProductServiceImpl(@Qualifier("productDaoMyBatis") ProductDao productDao,
                               @Qualifier("productAttachmentDaoMyBatis") ProductAttachmentDao productAttachmentDao) {
        this.productDao = productDao;
        this.productAttachmentDao = productAttachmentDao;
    }

    @Override
    public List<Product> findAll() {
        return productDao.findAll();
    }

    @Override
    public Product findById(Long id) {
        return productDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("找不到此商品"));
    }

    @Override
    public PageResponse<Product> findPage(int page, int size, String keyword) {
        List<Product> content = productDao.findPage(page, size, keyword);
        long totalElements = productDao.count(keyword);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }

    // 上傳檔案的同時建立新商品。
    // 檔案內容改成直接存進資料庫（product_attachment.file_data，BLOB），不再寫進本機硬碟——
    // 原因是本機硬碟在部署到雲端（例如 Render 免費方案）之後，應用程式一重啟就會被清空，
    // 檔案系統路徑的做法在那種環境下會導致附件憑空消失。
    // 副作用：product、product_attachment 這兩筆 DB 寫入，現在是唯一需要處理的動作，
    // 兩者都在同一個 @Transactional 裡，任何一筆失敗都會整個 rollback，
    // 不再需要像檔案系統版本那樣，額外在 catch 裡手動補刪「已經落地但資料庫沒紀錄」的孤兒檔案——
    // 因為現在檔案內容本身也是這個交易的一部分，rollback 自然就把它一起復原掉了。
    @Override
    @Transactional
    public Product createProductWithAttachment(Product product, MultipartFile file) {
        // 檔案改成選填：沒有選檔案就只新增商品本身，不建立附件紀錄
        boolean hasFile = file != null && !file.isEmpty();

        Product savedProduct = productDao.save(product);

        if (hasFile) {
            try {
                ProductAttachment attachment = new ProductAttachment();
                attachment.setProductId(savedProduct.getId());
                attachment.setOriginalFileName(file.getOriginalFilename());
                attachment.setFileData(file.getBytes());
                attachment.setFileSize(file.getSize());
                attachment.setContentType(file.getContentType());
                productAttachmentDao.save(attachment);
            } catch (IOException e) {
                // file.getBytes() 讀取上傳內容失敗（例如連線中斷），丟成 RuntimeException
                // 讓 @Transactional 依然能正確判斷要 rollback
                throw new IllegalStateException("讀取上傳檔案失敗: " + file.getOriginalFilename(), e);
            }
        }

        return savedProduct;
    }

    // 刪除商品：先刪 product_attachment，再刪 product 本身，兩者包在同一個 @Transactional 裡。
    // 如果商品還在別人的購物車裡（CartItem 對 Product 有 @ManyToOne 外鍵），
    // 刪 product 這一步會被資料庫的外鍵限制擋下來、丟 DataIntegrityViolationException，
    // 整個交易 rollback，attachment 也不會真的被刪掉。
    // 因為附件內容現在也是資料庫的一部分，這裡不再需要額外清理硬碟上的實體檔案。
    @Override
    @Transactional
    public void deleteProduct(Long id) {
        findById(id); // 商品不存在的話這裡就會丟 IllegalArgumentException("找不到此商品")
        productAttachmentDao.deleteByProductId(id);
        productDao.deleteById(id);
    }

    // 取得商品的附件（含檔案內容），找不到就丟例外（給 Controller 的附件讀取端點用）
    // 這裡是單一 SELECT，不是為了原子性才加 @Transactional，
    // readOnly = true 是告訴 Spring/Hibernate 這是唯讀查詢，可以省略 flush 之類的檢查，做一點效能優化
    @Override
    @Transactional(readOnly = true)
    public ProductAttachment getAttachment(Long productId) {
        List<ProductAttachment> attachments = productAttachmentDao.findByProductId(productId);
        if (attachments.isEmpty()) {
            throw new IllegalArgumentException("此商品沒有附件");
        }
        return attachments.get(0);
    }

    @Override
    @Transactional
    public ProductAttachment updateAttachment(Long productId, MultipartFile file) {
        findById(productId); // 商品不存在的話這裡就會丟例外，不會存出一筆孤兒附件
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("請選擇要上傳的檔案");
        }
        productAttachmentDao.deleteByProductId(productId); // 一個商品只留一筆附件，舊的先清掉再存新的
        try {
            ProductAttachment attachment = new ProductAttachment();
            attachment.setProductId(productId);
            attachment.setOriginalFileName(file.getOriginalFilename());
            attachment.setFileData(file.getBytes());
            attachment.setFileSize(file.getSize());
            attachment.setContentType(file.getContentType());
            return productAttachmentDao.save(attachment);
        } catch (IOException e) {
            throw new IllegalStateException("讀取上傳檔案失敗: " + file.getOriginalFilename(), e);
        }
    }
}
