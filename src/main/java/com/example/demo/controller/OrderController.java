package com.example.demo.controller;

import com.example.demo.model.Member;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItemDetail;
import com.example.demo.service.MemberService;
import com.example.demo.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final MemberService memberService;

    public OrderController(OrderService orderService, MemberService memberService) {
        this.orderService = orderService;
        this.memberService = memberService;
    }

    private Member getCurrentMember(HttpSession session) {
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) {
            throw new IllegalStateException("尚未登入");
        }
        return memberService.findById(memberId);
    }

    @GetMapping
    public ResponseEntity<List<Order>> getMyOrders(HttpSession session) {
        Member member = getCurrentMember(session);
        return ResponseEntity.ok(orderService.getOrdersForMember(member));
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<OrderItemDetail>> getOrderItems(@PathVariable Long id, HttpSession session) {
        Member member = getCurrentMember(session);
        return ResponseEntity.ok(orderService.getOrderItems(member, id));
    }
}