package com.example.demo.service.impl;

import com.example.demo.dao.OrderDao;
import com.example.demo.dao.OrderItemDao;
import com.example.demo.dao.ProductDao;
import com.example.demo.model.Member;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.model.OrderItemDetail;
import com.example.demo.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 測 OrderServiceImpl.getOrderItems 的「防 IDOR」：
 * 只能看自己的訂單，看別人的訂單要被擋下來。
 * 跟 CartServiceImplTest 一樣：用 Mockito 把三個 Dao 換成假物件，不連資料庫、不啟動 Spring。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderDao orderDao;
    @Mock
    private OrderItemDao orderItemDao;
    @Mock
    private ProductDao productDao;

    private OrderServiceImpl orderService;
    private Member member; // 目前登入的會員，id = 1

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderDao, orderItemDao, productDao);
        member = new Member();
        member.setId(1L);
    }

    private Order newOrder(Long orderId, Long ownerMemberId) {
        Order order = new Order();
        order.setId(orderId);
        order.setMemberId(ownerMemberId);
        return order;
    }

    private OrderItem newOrderItem(Long productId, int quantity, String unitPrice) {
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        return item;
    }

    @Test
    void getOrderItems_whenOrderBelongsToMember_shouldReturnDetailsWithProductName() {
        Order order = newOrder(7L, 1L); // 訂單擁有者 = 目前會員
        Product product = new Product();
        product.setId(10L);
        product.setName("鍵盤");

        when(orderDao.findById(7L)).thenReturn(Optional.of(order));
        when(orderItemDao.findByOrder(order)).thenReturn(List.of(newOrderItem(10L, 2, "100.00")));
        when(productDao.findById(10L)).thenReturn(Optional.of(product));

        List<OrderItemDetail> details = orderService.getOrderItems(member, 7L);

        assertEquals(1, details.size());
        assertEquals("鍵盤", details.get(0).getProductName());
        assertEquals(2, details.get(0).getQuantity());
    }

    @Test
    void getOrderItems_whenOrderBelongsToAnotherMember_shouldThrowAndNotLoadItems() {
        Order othersOrder = newOrder(7L, 2L); // 訂單擁有者 = 會員 2，不是目前的會員 1
        when(orderDao.findById(7L)).thenReturn(Optional.of(othersOrder));

        // 這就是 IDOR 防護：就算知道別人的訂單編號，也看不到
        assertThrows(IllegalStateException.class, () -> orderService.getOrderItems(member, 7L));

        // 而且連訂單明細都不應該被查出來，確保資料真的沒有外洩
        verify(orderItemDao, never()).findByOrder(any(Order.class));
    }

    @Test
    void getOrderItems_whenOrderDoesNotExist_shouldThrowIllegalArgument() {
        when(orderDao.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> orderService.getOrderItems(member, 999L));
    }

    @Test
    void getOrderItems_whenProductWasDeleted_shouldShowFallbackName() {
        Order order = newOrder(7L, 1L);
        when(orderDao.findById(7L)).thenReturn(Optional.of(order));
        when(orderItemDao.findByOrder(order)).thenReturn(List.of(newOrderItem(10L, 1, "100.00")));
        when(productDao.findById(10L)).thenReturn(Optional.empty()); // 商品已經被刪掉

        List<OrderItemDetail> details = orderService.getOrderItems(member, 7L);

        assertEquals("（商品已下架）", details.get(0).getProductName());
    }
}
