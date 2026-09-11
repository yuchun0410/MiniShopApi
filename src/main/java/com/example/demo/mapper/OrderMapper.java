package com.example.demo.mapper;

import com.example.demo.model.Order;
import java.util.List;

public interface OrderMapper {
    int insert(Order order);
    Order findById(Long id);
    List<Order> findByMemberId(Long memberId);
}