package com.example.demo.repository;

import com.example.demo.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 注意：Order 現在是用 memberId（Long）這個外鍵欄位，不是 @ManyToOne Member 物件關聯，
// 所以衍生查詢方法要寫成 findByMemberId，不能再寫 findByMember(Member member) 了——
// 這正是之前 findByMember(Member) 讓應用程式啟動失敗的原因。
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByMemberId(Long memberId);
}
