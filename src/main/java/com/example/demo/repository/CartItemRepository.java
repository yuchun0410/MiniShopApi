package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.CartItem;
import com.example.demo.model.Member;
import com.example.demo.model.Product;
@Repository
public interface CartItemRepository extends JpaRepository<CartItem,Long>{
	// 查某個會員購物車裡的所有項目
    List<CartItem> findByMember(Member member);

    // 加入購物車前先檢查:這個會員的購物車裡是否已經有這個商品了
    Optional<CartItem> findByMemberAndProduct(Member member, Product product);

    // 登出或清空購物車時可能用到
    void deleteByMember(Member member);
}
