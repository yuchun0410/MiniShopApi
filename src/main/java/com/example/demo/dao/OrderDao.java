package com.example.demo.dao;

import com.example.demo.model.Member;
import com.example.demo.model.Order;
import java.util.List;
import java.util.Optional;

public interface OrderDao {
    Order save(Order order);
    List<Order> findByMember(Member member);
    Optional<Order> findById(Long id);
}