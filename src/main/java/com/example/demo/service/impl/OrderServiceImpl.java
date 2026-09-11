package com.example.demo.service.impl;

import com.example.demo.dao.OrderDao;
import com.example.demo.dao.OrderItemDao;
import com.example.demo.dao.ProductDao;
import org.springframework.beans.factory.annotation.Qualifier;
import com.example.demo.model.Member;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.model.OrderItemDetail;
import com.example.demo.model.Product;
import com.example.demo.service.OrderService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;
    private final ProductDao productDao;

    public OrderServiceImpl(@Qualifier("orderDaoMyBatis") OrderDao orderDao,
                            @Qualifier("orderItemDaoMyBatis") OrderItemDao orderItemDao,
                            @Qualifier("productDaoMyBatis") ProductDao productDao) {
        this.orderDao = orderDao;
        this.orderItemDao = orderItemDao;
        this.productDao = productDao;
    }

    @Override
    public List<Order> getOrdersForMember(Member member) {
        return orderDao.findByMember(member);
    }

    @Override
    public List<OrderItemDetail> getOrderItems(Member member, Long orderId) {
        Order order = orderDao.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("找不到此訂單"));

        if (!order.getMemberId().equals(member.getId())) {
            throw new IllegalStateException("無權查看此訂單");
        }

        List<OrderItem> items = orderItemDao.findByOrder(order);

        // OrderItem 本身只存 productId，沒有商品名稱（跟 CartItem 不一樣，CartItem 還留著真的物件關聯）。
        // 這裡逐筆查一次商品名稱組成 DTO 回傳，商品被刪掉的情況也給個友善文字，不讓前端拿到 null。
        return items.stream()
                .map(item -> {
                    String productName = productDao.findById(item.getProductId())
                            .map(Product::getName)
                            .orElse("（商品已下架）");
                    return new OrderItemDetail(item.getId(), item.getProductId(), productName,
                            item.getQuantity(), item.getUnitPrice());
                })
                .collect(Collectors.toList());
    }
}
