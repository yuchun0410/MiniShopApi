package com.example.demo.service.impl;

import com.example.demo.dao.CartItemDao;
import com.example.demo.dao.OrderDao;
import com.example.demo.dao.OrderItemDao;
import com.example.demo.dao.ProductDao;
import org.springframework.beans.factory.annotation.Qualifier;
import com.example.demo.model.*;
import com.example.demo.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartItemDao cartItemDao;
    private final ProductDao productDao;
    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;

    public CartServiceImpl(@Qualifier("cartItemDaoMyBatis") CartItemDao cartItemDao,
                            @Qualifier("productDaoMyBatis") ProductDao productDao,
                            @Qualifier("orderDaoMyBatis") OrderDao orderDao,
                            @Qualifier("orderItemDaoMyBatis") OrderItemDao orderItemDao) {
        this.cartItemDao = cartItemDao;
        this.productDao = productDao;
        this.orderDao = orderDao;
        this.orderItemDao = orderItemDao;
    }

    @Override
    public CartItem addToCart(Member member, Long productId, int quantity) {
        Product product = productDao.findById(productId)
                .orElseThrow(() -> {
                    log.warn("加入購物車失敗，找不到商品，memberId={}, productId={}", member.getId(), productId);
                    return new IllegalArgumentException("找不到此商品");
                });

        CartItem result = cartItemDao.findByMemberAndProduct(member, product)
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity() + quantity);
                    return cartItemDao.save(existing);
                })
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setMember(member);
                    newItem.setProduct(product);
                    newItem.setQuantity(quantity);
                    return cartItemDao.save(newItem);
                });

        log.info("加入購物車成功，memberId={}, productId={}, quantity={}", member.getId(), productId, quantity);
        return result;
    }

    @Override
    public List<CartItem> getCart(Member member) {
        return cartItemDao.findByMember(member);
    }

    @Override
    public CartItem updateQuantity(Member member, Long cartItemId, int quantity) {
        // 不直接用 cartItemDao.findById(cartItemId) 去比對擁有者，
        // 因為 CartItem 的 MyBatis 版 resultMap 沒有把 member 這個關聯查出來（member 會是 null）。
        // 改成先撈「這個會員自己的購物車」，再從裡面找這筆項目——查得到就代表本來就是他的，
        // 這樣不管底層是 MyBatis 還是 JPA 都能正確做到擁有者檢查。
        CartItem item = cartItemDao.findByMember(member).stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("修改數量失敗，找不到購物車項目或非本人所有，memberId={}, cartItemId={}", member.getId(), cartItemId);
                    return new IllegalArgumentException("找不到此購物車項目");
                });
        item.setQuantity(quantity);
        CartItem saved = cartItemDao.save(item);
        log.info("修改購物車數量成功，cartItemId={}, quantity={}", cartItemId, quantity);
        return saved;
    }

    @Override
    public void removeItem(Member member, Long cartItemId) {
        boolean owns = cartItemDao.findByMember(member).stream()
                .anyMatch(i -> i.getId().equals(cartItemId));
        if (!owns) {
            log.warn("刪除購物車項目失敗，找不到此項目或非本人所有，memberId={}, cartItemId={}", member.getId(), cartItemId);
            throw new IllegalArgumentException("找不到此購物車項目");
        }
        cartItemDao.deleteById(cartItemId);
        log.info("移除購物車項目，cartItemId={}", cartItemId);
    }

    @Override
    @Transactional
    public Order checkout(Member member) {
        List<CartItem> items = cartItemDao.findByMember(member);
        if (items.isEmpty()) {
            throw new IllegalStateException("購物車是空的，無法結帳");
        }

        // 結帳前先檢查每一項的庫存夠不夠，不夠就直接擋下來、不建立訂單。
        // 註：這裡只是「檢查完再扣」的簡單版本，沒有處理「兩個人同時搶最後一件庫存」的併發情境
        // ——真的要做到嚴謹的話，需要在 Product 上加樂觀鎖（@Version）或悲觀鎖（SELECT ... FOR UPDATE）。
        for (CartItem item : items) {
            Product product = item.getProduct();
            if (product.getStock() < item.getQuantity()) {
                log.warn("結帳失敗，庫存不足，memberId={}, productId={}, 需要數量={}, 目前庫存={}",
                        member.getId(), product.getId(), item.getQuantity(), product.getStock());
                throw new IllegalStateException("庫存不足：" + product.getName());
            }
        }

        BigDecimal total = items.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setMemberId(member.getId());
        order.setTotalAmount(total);
        order = orderDao.save(order);

        for (CartItem item : items) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(item.getProduct().getId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPrice(item.getProduct().getPrice());
            orderItemDao.save(orderItem);

            // 結帳成功才真的把庫存扣掉（加入購物車的階段不扣，只有真的結帳完成才算數）
            // 用資料庫原子扣庫存：stock >= 數量才會扣，扣不到（回傳 false）代表剛好被別人買走，丟例外讓整筆交易 rollback
            Product product = item.getProduct();
            if (!productDao.decreaseStock(product.getId(), item.getQuantity())) {
                log.warn("結帳失敗，扣庫存時庫存不足，memberId={}, productId={}, 需要數量={}",
                        member.getId(), product.getId(), item.getQuantity());
                throw new IllegalStateException("庫存不足：" + product.getName());
            }
        }

        cartItemDao.deleteByMember(member);

        log.info("結帳成功，orderId={}, memberId={}, total={}", order.getId(), member.getId(), total);
        return order;
    }
}
