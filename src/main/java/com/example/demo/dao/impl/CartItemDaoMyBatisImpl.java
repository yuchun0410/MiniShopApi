package com.example.demo.dao.impl;

import com.example.demo.dao.CartItemDao;
import com.example.demo.mapper.CartItemMapper;
import com.example.demo.model.CartItem;
import com.example.demo.model.Member;
import com.example.demo.model.Product;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("cartItemDaoMyBatis")
public class CartItemDaoMyBatisImpl implements CartItemDao {

    private final CartItemMapper cartItemMapper;

    public CartItemDaoMyBatisImpl(CartItemMapper cartItemMapper) {
        this.cartItemMapper = cartItemMapper;
    }

    @Override
    public List<CartItem> findByMember(Member member) {
        return cartItemMapper.findByMemberId(member.getId());
    }

    @Override
    public Optional<CartItem> findByMemberAndProduct(Member member, Product product) {
        return Optional.ofNullable(cartItemMapper.findByMemberAndProduct(member.getId(), product.getId()));
    }

    @Override
    public CartItem save(CartItem item) {
        if (item.getId() == null) {
            cartItemMapper.insert(item);
        } else {
            cartItemMapper.update(item);
        }
        return item;
    }

    @Override
    public Optional<CartItem> findById(Long id) {
        return Optional.ofNullable(cartItemMapper.findById(id));
    }

    @Override
    public void deleteById(Long id) {
        cartItemMapper.deleteById(id);
    }

    @Override
    public void deleteByMember(Member member) {
        cartItemMapper.deleteByMemberId(member.getId());
    }
}
