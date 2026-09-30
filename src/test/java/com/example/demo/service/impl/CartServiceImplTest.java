package com.example.demo.service.impl;

import com.example.demo.dao.CartItemDao;
import com.example.demo.dao.OrderDao;
import com.example.demo.dao.OrderItemDao;
import com.example.demo.dao.ProductDao;
import com.example.demo.model.CartItem;
import com.example.demo.model.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
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
}
