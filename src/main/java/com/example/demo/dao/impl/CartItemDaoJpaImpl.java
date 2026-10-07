package com.example.demo.dao.impl;

import com.example.demo.dao.CartItemDao;
import com.example.demo.model.CartItem;
import com.example.demo.model.Member;
import com.example.demo.model.Product;
import com.example.demo.repository.CartItemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// CartItem 這個 entity 還是保留真正的 @ManyToOne Member / Product 關聯（沒有像 Order 那樣改成外鍵 ID），
// 所以 CartItemRepository 上用 Member、Product 物件本身當參數的衍生查詢方法，可以直接沿用，不用改。
@Repository("cartItemDaoJpa")
public class CartItemDaoJpaImpl implements CartItemDao {

    private final CartItemRepository cartItemRepository;

    public CartItemDaoJpaImpl(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    @Override
    public List<CartItem> findByMember(Member member) {
        return cartItemRepository.findByMember(member);
    }

    @Override
    public Optional<CartItem> findByMemberAndProduct(Member member, Product product) {
        return cartItemRepository.findByMemberAndProduct(member, product);
    }

    @Override
    public CartItem save(CartItem item) {
        return cartItemRepository.save(item);
    }

    @Override
    public void deleteById(Long id) {
        cartItemRepository.deleteById(id);
    }

    @Override
    public void deleteByMember(Member member) {
        cartItemRepository.deleteByMember(member);
    }
}
