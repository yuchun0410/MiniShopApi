package com.example.demo.controller;

import jakarta.servlet.http.HttpSession;
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

    private final CartService cartService;
    private final MemberService memberService;

    public CartController(CartService cartService, MemberService memberService) {
        this.cartService = cartService;
        this.memberService = memberService;
    }

    private Member getCurrentMember(HttpSession session) {
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) {
            throw new IllegalStateException("尚未登入");
        }
        return memberService.findById(memberId);
    }

    @GetMapping
    public ResponseEntity<List<CartItem>> getCart(HttpSession session) {
        Member member = getCurrentMember(session);
        return ResponseEntity.ok(cartService.getCart(member));
    }

    @PostMapping
    public ResponseEntity<CartItem> addToCart(@RequestParam Long productId,
                                               @RequestParam Integer quantity,
                                               HttpSession session) {
        Member member = getCurrentMember(session);
        CartItem item = cartService.addToCart(member, productId, quantity);
        return ResponseEntity.ok(item);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartItem> updateQuantity(@PathVariable Long id,
                                                    @RequestParam Integer quantity,
                                                    HttpSession session) {
        Member member = getCurrentMember(session);
        CartItem item = cartService.updateQuantity(member, id, quantity);
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeItem(@PathVariable Long id, HttpSession session) {
        Member member = getCurrentMember(session);
        cartService.removeItem(member, id);
        return ResponseEntity.ok().build();
    }

    // 結帳（先簡化成：清空購物車，之後要做訂單記錄可以在這裡擴充）
    // 結帳：建立訂單 + 訂單明細，並清空購物車
    @PostMapping("/checkout")
    public ResponseEntity<com.example.demo.model.Order> checkout(HttpSession session) {
        Member member = getCurrentMember(session);
        com.example.demo.model.Order order = cartService.checkout(member);
        return ResponseEntity.ok(order);
    }
}