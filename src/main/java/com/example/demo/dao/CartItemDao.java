package com.example.demo.dao;

import com.example.demo.model.CartItem;
import com.example.demo.model.Member;
import com.example.demo.model.Product;
import java.util.List;
import java.util.Optional;

public interface CartItemDao {
    List<CartItem> findByMember(Member member);
    Optional<CartItem> findByMemberAndProduct(Member member, Product product);
    CartItem save(CartItem item);
    void deleteById(Long id);
    void deleteByMember(Member member);
}