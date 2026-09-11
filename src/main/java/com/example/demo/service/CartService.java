package com.example.demo.service;

import com.example.demo.model.CartItem;
import com.example.demo.model.Member;
import com.example.demo.model.Order;
import java.util.List;

public interface CartService {
    CartItem addToCart(Member member, Long productId, int quantity);
    List<CartItem> getCart(Member member);
    CartItem updateQuantity(Member member, Long cartItemId, int quantity);
    void removeItem(Member member, Long cartItemId);
    Order checkout(Member member);
}
