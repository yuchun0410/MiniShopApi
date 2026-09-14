package com.example.demo.service.impl;

import com.example.demo.dao.ProductAttachmentDao;
import com.example.demo.dao.ProductDao;
import com.example.demo.model.PageResponse;
import com.example.demo.model.Product;
import com.example.demo.model.ProductAttachment;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.ProductService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductDao productDao;
    private final ProductAttachmentDao productAttachmentDao;
    private final FileStorageService fileStorageService;

    // ProductDao / ProductAttachmentDao 都各自有兩個實作（MyBatis / JPA），
    // 用 @Qualifier 明確指定要注入哪一個，避免 Spring 因為有多個候選 bean而丟出 NoUniqueBeanDefinitionException。
    // 目前都指定用 MyBatis 版，維持跟其他模組一致；要切換成 JPA 版，把下面字串改成對應的 "...Jpa" 即可。
    public ProductServiceImpl(@Qualifier("productDaoMyBatis") ProductDao productDao,
                               @Qualifier("productAttachmentDaoMyBatis") ProductAttachmentDao productAttachmentDao,
                               FileStorageService fileStorageService) {
        this.productDao = productDao;
        this.productAttachmentDao = productAttachmentDao;
        this.fileStorageService = fileStorageService;
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

    // 上傳檔案的同時建立新商品，示範 @Transactional：
    // - product、product_attachment 這兩筆 DB 寫入包在同一個交易裡：要嘛都成功，要嘛都失敗一起 rollback。
    // - 但檔案落地硬碟這件事不受 @Transactional 管（它不是 JDBC 操作），
    //   所以如果檔案寫完之後、DB 那步才失敗，交易會自動把 DB 復原，
    //   但硬碟上的檔案不會自動消失，因此在 catch 裡手動呼叫 fileStorageService.delete() 補刪，
    //   避免留下沒有對應資料庫紀錄的「孤兒檔案」。
    @Override
    @Transactional
    public Product createProductWithAttachment(Product product, MultipartFile file) {
        // 檔案改成選填：沒有選檔案就只新增商品本身，不建立附件紀錄
        boolean hasFile = file != null && !file.isEmpty();

        String storedFileName = null;
        String storedPath = null;
        if (hasFile) {
            storedFileName = fileStorageService.generateStoredFileName(file.getOriginalFilename());
            storedPath = fileStorageService.store(file, storedFileName);
        }

        try {
            Product savedProduct = productDao.save(product);

            if (hasFile) {
                ProductAttachment attachment = new ProductAttachment();
                attachment.setProductId(savedProduct.getId());
                attachment.setOriginalFileName(file.getOriginalFilename());
                attachment.setStoredFileName(storedFileName);
                attachment.setFilePath(storedPath);
                attachment.setFileSize(file.getSize());
                productAttachmentDao.save(attachment);
            }

            return savedProduct;
        } catch (RuntimeException e) {
            // DB 的部分 Spring 會自動 rollback，但硬碟上的檔案不會，這裡手動補刪（沒有檔案的話這步不用做）
            if (hasFile) {
                fileStorageService.delete(storedPath);
            }
            throw e;
        }
    }

    // 刪除商品，示範跟上傳相反方向的「DB 與檔案系統順序」問題：
    // - 先刪 product_attachment，再刪 product 本身，兩者包在同一個 @Transactional 裡。
    //   如果商品還在別人的購物車裡（CartItem 對 Product 有 @ManyToOne 外鍵），
    //   刪 product 這一步會被資料庫的外鍵限制擋下來、丟 DataIntegrityViolationException，
    //   整個交易 rollback，後面刪實體檔案的程式碼根本不會執行到，檔案不會受影響。
    // - 只有 DB 這兩筆都刪成功、方法正常執行完，才會在最後把硬碟上的實體檔案刪掉。
    //   這樣萬一刪檔案失敗，頂多留下沒人參照的孤兒檔案（可回收），
    //   不會發生「資料庫還有紀錄、但檔案已經被刪掉」這種更麻煩的斷鏈情況。
    @Override
    @Transactional
    public void deleteProduct(Long id) {
        findById(id); // 商品不存在的話這裡就會丟 IllegalArgumentException("找不到此商品")
        List<ProductAttachment> attachments = productAttachmentDao.findByProductId(id);

        productAttachmentDao.deleteByProductId(id);
        productDao.deleteById(id);

        for (ProductAttachment attachment : attachments) {
            fileStorageService.delete(attachment.getFilePath());
        }
    }
}
