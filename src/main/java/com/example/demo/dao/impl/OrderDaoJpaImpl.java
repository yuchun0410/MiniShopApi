package com.example.demo.dao.impl;

import com.example.demo.dao.OrderDao;
import com.example.demo.model.Member;
import com.example.demo.model.Order;
import com.example.demo.repository.OrderRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Order 已經沒有 @PrePersist 了（MyBatis 轉換時拿掉的，因為 MyBatis 不會觸發 JPA 生命週期），
// 所以這個 JPA 版本一樣要手動設定 orderDate，跟 MyBatis 版本行為保持一致。
@Repository("orderDaoJpa")
public class OrderDaoJpaImpl implements OrderDao {

    private final OrderRepository orderRepository;

    public OrderDaoJpaImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Order save(Order order) {
        order.setOrderDate(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Override
    public List<Order> findByMember(Member member) {
        return orderRepository.findByMemberId(member.getId());
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }
}
