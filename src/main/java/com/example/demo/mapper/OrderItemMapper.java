package com.example.demo.mapper;

import com.example.demo.model.OrderItem;
import com.example.demo.model.ProductSalesReport;
import java.util.List;

public interface OrderItemMapper {
    int insert(OrderItem item);
    List<OrderItem> findByOrderId(Long orderId);
    List<ProductSalesReport> findProductSalesReport();
}