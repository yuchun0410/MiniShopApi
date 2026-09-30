package com.example.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.CartItem;
import com.example.demo.model.Member;
import com.example.demo.service.CartService;
import com.example.demo.service.MemberService;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;
    private final MemberService memberService;

    public CartController(CartService cartService, MemberService memberService) {
        this.cartService = cartService;
        this.memberService = memberService;
    }

    private Member getCurrentMember(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            log.warn("未登入狀態嘗試存取購物車相關 API，uri={}", request.getRequestURI());
            throw new IllegalStateException("尚未登入");
        }
        return memberService.findById(memberId);
    }

    @GetMapping
    public ResponseEntity<List<CartItem>> getCart(HttpServletRequest request) {
        Member member = getCurrentMember(request);
        return ResponseEntity.ok(cartService.getCart(member));
    }

    @PostMapping
    public ResponseEntity<CartItem> addToCart(@RequestParam Long productId,
                                               @RequestParam Integer quantity,
                                               HttpServletRequest request) {
        Member member = getCurrentMember(request);
        CartItem item = cartService.addToCart(member, productId, quantity);
        return ResponseEntity.ok(item);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartItem> updateQuantity(@PathVariable Long id,
                                                    @RequestParam Integer quantity,
                                                    HttpServletRequest request) {
        Member member = getCurrentMember(request);
        CartItem item = cartService.updateQuantity(member, id, quantity);
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeItem(@PathVariable Long id, HttpServletRequest request) {
        Member member = getCurrentMember(request);
        cartService.removeItem(member, id);
        return ResponseEntity.ok().build();
    }

    // 結帳（先簡化成：清空購物車，之後要做訂單記錄可以在這裡擴充）
    // 結帳：建立訂單 + 訂單明細，並清空購物車
    @PostMapping("/checkout")
    public ResponseEntity<com.example.demo.model.Order> checkout(HttpServletRequest request) {
        Member member = getCurrentMember(request);
        com.example.demo.model.Order order = cartService.checkout(member);
        return ResponseEntity.ok(order);
    }
}
