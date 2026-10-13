package com.example.demo.service.impl;

import com.example.demo.dao.CartItemDao;
import com.example.demo.dao.OrderDao;
import com.example.demo.dao.OrderItemDao;
import com.example.demo.dao.ProductDao;
import com.example.demo.model.CartItem;
import com.example.demo.model.Member;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 這是嚴格定義下的「單元測試」(Unit Test)：
 * 只測 CartServiceImpl 這個類別自己的邏輯，完全不連真的資料庫、不啟動 Spring 容器。
 * 用 Mockito 把它依賴的四個 Dao 全部換成假物件（mock），自己控制假物件要回傳什麼，
 * 藉此單獨驗證「這段商業邏輯」對不對。
 *
 * 跟專案裡另一個 MemberMapperTest（用 @SpringBootTest 真的連 MySQL）不一樣，
 * 那個是「整合測試」(Integration Test)——會啟動整個 Spring 容器、真的打資料庫。
 * 這裡才是不需要任何外部服務、跑起來是毫秒等級的「純」單元測試。
 */
@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartItemDao cartItemDao;
    @Mock
    private ProductDao productDao;
    @Mock
    private OrderDao orderDao;
    @Mock
    private OrderItemDao orderItemDao;

    private CartServiceImpl cartService;
    private Member member;

    @BeforeEach
    void setUp() {
        // 手動 new 出來，把四個假 Dao 灌進建構子——不需要 Spring 容器幫我們組裝
        cartService = new CartServiceImpl(cartItemDao, productDao, orderDao, orderItemDao);

        member = new Member();
        member.setId(1L);
    }

    @Test
    void updateQuantity_whenItemBelongsToMember_shouldUpdateSuccessfully() {
        // 準備一筆「屬於這個會員」的購物車項目
        CartItem item = new CartItem();
        item.setId(100L);
        item.setQuantity(1);

        // 假 Dao：呼叫 findByMember(member) 時回傳這個清單
        when(cartItemDao.findByMember(member)).thenReturn(List.of(item));
        // 假 Dao：呼叫 save(item) 時把傳進來的物件原封不動回傳
        when(cartItemDao.save(item)).thenReturn(item);

        CartItem result = cartService.updateQuantity(member, 100L, 5);

        assertEquals(5, result.getQuantity());
        verify(cartItemDao, times(1)).save(item);
    }

    @Test
    void updateQuantity_whenItemDoesNotBelongToMember_shouldThrowException() {
        // 這個會員的購物車裡根本沒有 cartItemId=999 這筆（可能是別人的購物車項目）
        when(cartItemDao.findByMember(member)).thenReturn(List.of());

        // 應該拋出例外，而不是讓他改到別人的購物車——這就是 IDOR 防護在起作用
        assertThrows(IllegalArgumentException.class,
                () -> cartService.updateQuantity(member, 999L, 5));

        // 而且絕對不能呼叫到 save，確保真的被擋下來
        verify(cartItemDao, never()).save(any());
    }

    // ======================= checkout（結帳）=======================
    // 重點：@Transactional 的「回滾」本身需要連真的資料庫才驗證得到（整合測試）。
    // 單元測試能驗證的是：「該丟例外的時候有丟、不該做的事沒有做」——
    // 因為丟出例外，正是觸發回滾的條件。

    private Product newProduct(Long id, String name, String price, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setStock(stock);
        return p;
    }

    private CartItem newCartItem(Product product, int quantity) {
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        return item;
    }

    @Test
    void checkout_whenCartIsEmpty_shouldThrowAndNotCreateOrder() {
        when(cartItemDao.findByMember(member)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> cartService.checkout(member));

        verify(orderDao, never()).save(any(Order.class));
    }

    @Test
    void checkout_whenStockIsEnough_shouldCreateOrderDecreaseStockAndClearCart() {
        Product product = newProduct(10L, "鍵盤", "100.00", 5);
        when(cartItemDao.findByMember(member)).thenReturn(List.of(newCartItem(product, 2)));
        // 假的 orderDao.save：模擬資料庫幫新訂單產生 id（500），再把訂單回傳
        when(orderDao.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(500L);
            return o;
        });
        // 假的原子扣庫存：回傳 true 代表資料庫真的扣成功
        when(productDao.decreaseStock(10L, 2)).thenReturn(true);

        Order result = cartService.checkout(member);

        assertEquals(500L, result.getId());
        assertEquals(1L, result.getMemberId());
        // BigDecimal 用 compareTo 比大小，避免 200.0 與 200.00 因 scale 不同被判成不相等
        assertEquals(0, new BigDecimal("200.00").compareTo(result.getTotalAmount()));
        verify(orderItemDao, times(1)).save(any(OrderItem.class));
        verify(productDao, times(1)).decreaseStock(10L, 2);
        verify(cartItemDao, times(1)).deleteByMember(member);
    }

    @Test
    void checkout_whenStockIsNotEnough_shouldThrowAndNotCreateOrder() {
        Product product = newProduct(10L, "鍵盤", "100.00", 1);
        when(cartItemDao.findByMember(member)).thenReturn(List.of(newCartItem(product, 3)));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> cartService.checkout(member));

        assertTrue(ex.getMessage().contains("鍵盤"));
        // 庫存不足要「在建立訂單之前」就擋下來：沒有訂單、沒有扣庫存、購物車也沒被清空
        verify(orderDao, never()).save(any(Order.class));
        verify(productDao, never()).decreaseStock(anyLong(), anyInt());
        verify(cartItemDao, never()).deleteByMember(any(Member.class));
    }

    @Test
    void checkout_whenAtomicDecreaseFails_shouldThrowAndNotClearCart() {
        // 預先檢查時庫存看起來夠（5 >= 2），但真正扣庫存時資料庫回傳 false
        // —— 模擬「在檢查和扣庫存之間，剛好被別人買走」的搶購情境
        Product product = newProduct(10L, "鍵盤", "100.00", 5);
        when(cartItemDao.findByMember(member)).thenReturn(List.of(newCartItem(product, 2)));
        when(orderDao.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(500L);
            return o;
        });
        when(productDao.decreaseStock(10L, 2)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> cartService.checkout(member));

        // 沒有走到「清空購物車」那一步。
        // 真正的 rollback（把已經存進去的訂單撤銷）是 @Transactional 的工作，
        // 要用整合測試連真的資料庫才驗證得到。
        verify(cartItemDao, never()).deleteByMember(any(Member.class));
    }
}
