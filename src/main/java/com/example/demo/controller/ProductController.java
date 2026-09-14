package com.example.demo.controller;

import com.example.demo.model.Member;
import com.example.demo.model.PageResponse;
import com.example.demo.model.Product;
import com.example.demo.service.MemberService;
import com.example.demo.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final MemberService memberService;

    public ProductController(ProductService productService, MemberService memberService) {
        this.productService = productService;
        this.memberService = memberService;
    }

    // 換頁 + 查詢: /api/products?page=1&size=10&keyword=滑鼠
    // keyword 不帶或帶空字串，就是不篩選，回傳全部商品的分頁結果
    @GetMapping
    public PageResponse<Product> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return productService.findPage(page, size, keyword);
    }

    @GetMapping("/{id}")
    public Product getOne(@PathVariable Long id) {
        return productService.findById(id);
    }

    // 上架新商品 + 上傳附件檔案（PDF / Excel / 圖片皆可，不限格式）：只有管理員能操作
    // 用 multipart/form-data 傳，name/price/stock 是一般欄位，file 是檔案本體
    // 商品 + 附件這兩筆資料庫寫入包在同一個 @Transactional 裡（見 ProductServiceImpl）
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> createWithAttachment(
            @RequestParam String name,
            @RequestParam BigDecimal price,
            @RequestParam Integer stock,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) {
        requireAdmin(request);

        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setStock(stock);

        Product saved = productService.createProductWithAttachment(product, file);
        return ResponseEntity.ok(saved);
    }

    // 刪除商品：只有管理員能操作。先刪商品 + 附件的 DB 紀錄，成功後才刪硬碟上的實體檔案（見 ProductServiceImpl）
    // 如果這個商品還在別人的購物車裡，資料庫外鍵限制會擋下來，變成 400（見 GlobalExceptionHandler）
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        productService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }

    // 共用檢查：沒登入 -> 丟例外讓 GlobalExceptionHandler 轉成 401；不是管理員 -> 轉成 403
    private void requireAdmin(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            throw new IllegalStateException("尚未登入");
        }
        Member current = memberService.findById(memberId);
        if (current.getRole() != com.example.demo.model.Role.ADMIN) {
            throw new com.example.demo.exception.AccessDeniedException("需要管理員權限");
        }
    }
}
