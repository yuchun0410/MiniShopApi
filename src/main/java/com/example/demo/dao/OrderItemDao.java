package com.example.demo.dao;

import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.model.ProductSalesReport;
import java.util.List;

public interface OrderItemDao {
    OrderItem save(OrderItem item);
    List<OrderItem> findByOrder(Order order);
    List<ProductSalesReport> findProductSalesReport();
}