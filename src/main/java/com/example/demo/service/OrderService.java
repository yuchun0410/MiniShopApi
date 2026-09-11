package com.example.demo.service;

import com.example.demo.model.Member;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItemDetail;
import java.util.List;

public interface OrderService {
    List<Order> getOrdersForMember(Member member);
    List<OrderItemDetail> getOrderItems(Member member, Long orderId);
}
