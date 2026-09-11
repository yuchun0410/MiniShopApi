package com.example.demo.repository;

import com.example.demo.model.OrderItem;
import com.example.demo.model.ProductSalesReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// 同樣道理：OrderItem 現在是用 orderId / productId（Long）這兩個外鍵欄位，
// 不是 @ManyToOne 物件關聯，所以衍生查詢要寫 findByOrderId，不能寫 findByOrder(Order order)。
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    // 這裡是這次重構比較關鍵的地方：因為 OrderItem 已經沒有 oi.product 這種物件關聯可以直接 JOIN，
    // 只剩下 productId 這個純數字欄位，所以不能像轉換 MyBatis 之前那樣寫 "FROM OrderItem oi JOIN oi.product p"。
    // 改用 JPA 2.1 之後支援的「ON 子句」寫法，直接用 oi.productId = p.id 手動關聯兩個不相關的 entity。
    // ProductSalesReport 建構子的參數順序，要跟這裡 new 出來的順序完全對上。
    @Query("SELECT new com.example.demo.model.ProductSalesReport(oi.productId, p.name, SUM(oi.quantity), SUM(oi.quantity * oi.unitPrice)) " +
           "FROM OrderItem oi JOIN Product p ON p.id = oi.productId " +
           "GROUP BY oi.productId, p.name " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<ProductSalesReport> findProductSalesReport();
}
