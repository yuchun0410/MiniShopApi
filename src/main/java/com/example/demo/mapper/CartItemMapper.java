package com.example.demo.mapper;

import com.example.demo.model.CartItem;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface CartItemMapper {
    CartItem findById(Long id);
    List<CartItem> findByMemberId(@Param("memberId") Long memberId);
    CartItem findByMemberAndProduct(@Param("memberId") Long memberId, @Param("productId") Long productId);
    int insert(CartItem item);
    int update(CartItem item);
    int deleteById(Long id);
    int deleteByMemberId(Long memberId);
}