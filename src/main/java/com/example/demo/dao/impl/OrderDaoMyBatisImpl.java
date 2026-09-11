package com.example.demo.dao.impl;

import com.example.demo.dao.OrderDao;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.model.Member;
import com.example.demo.model.Order;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository("orderDaoMyBatis")
public class OrderDaoMyBatisImpl implements OrderDao {

    private final OrderMapper orderMapper;

    public OrderDaoMyBatisImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public Order save(Order order) {
        order.setOrderDate(LocalDateTime.now());
        orderMapper.insert(order);
        return order;
    }

    @Override
    public List<Order> findByMember(Member member) {
        return orderMapper.findByMemberId(member.getId());
    }

    @Override
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(orderMapper.findById(id));
    }
}
