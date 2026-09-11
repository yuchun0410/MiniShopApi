package com.example.demo.dao.impl;

import com.example.demo.dao.OrderItemDao;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.model.ProductSalesReport;
import com.example.demo.repository.OrderItemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("orderItemDaoJpa")
public class OrderItemDaoJpaImpl implements OrderItemDao {

    private final OrderItemRepository orderItemRepository;

    public OrderItemDaoJpaImpl(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public OrderItem save(OrderItem item) {
        return orderItemRepository.save(item);
    }

    @Override
    public List<OrderItem> findByOrder(Order order) {
        return orderItemRepository.findByOrderId(order.getId());
    }

    @Override
    public List<ProductSalesReport> findProductSalesReport() {
        return orderItemRepository.findProductSalesReport();
    }
}
