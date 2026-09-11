package com.example.demo.dao.impl;

import com.example.demo.dao.OrderItemDao;
import com.example.demo.mapper.OrderItemMapper;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.model.ProductSalesReport;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("orderItemDaoMyBatis")
public class OrderItemDaoMyBatisImpl implements OrderItemDao {

    private final OrderItemMapper orderItemMapper;

    public OrderItemDaoMyBatisImpl(OrderItemMapper orderItemMapper) {
        this.orderItemMapper = orderItemMapper;
    }

    @Override
    public OrderItem save(OrderItem item) {
        orderItemMapper.insert(item);
        return item;
    }

    @Override
    public List<OrderItem> findByOrder(Order order) {
        return orderItemMapper.findByOrderId(order.getId());
    }

    @Override
    public List<ProductSalesReport> findProductSalesReport() {
        return orderItemMapper.findProductSalesReport();
    }
}
