package com.example.demo.service;

import com.example.demo.model.PageResponse;
import com.example.demo.model.Product;
import com.example.demo.model.ProductAttachment;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ProductService {
    List<Product> findAll();
    Product findById(Long id);
    PageResponse<Product> findPage(int page, int size, String keyword);

    // 上傳檔案的同時建立新商品：示範 @Transactional 保護商品 + 附件這兩筆 DB 寫入的原子性
    Product createProductWithAttachment(Product product, MultipartFile file);

    // 刪除商品：先刪 DB（商品 + 附件紀錄），交易成功後才刪硬碟上的實體檔案
    void deleteProduct(Long id);

    // 取得商品的附件中繼資料，給 Controller 讀檔用
    ProductAttachment getAttachment(Long productId);
}
