package com.example.demo.controller;

import com.example.demo.model.Member;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItemDetail;
import com.example.demo.service.MemberService;
import com.example.demo.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
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

    private Member getCurrentMember(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            throw new IllegalStateException("尚未登入");
        }
        return memberService.findById(memberId);
    }

    @GetMapping
    public ResponseEntity<List<Order>> getMyOrders(HttpServletRequest request) {
        Member member = getCurrentMember(request);
        return ResponseEntity.ok(orderService.getOrdersForMember(member));
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<List<OrderItemDetail>> getOrderItems(@PathVariable Long id, HttpServletRequest request) {
        Member member = getCurrentMember(request);
        return ResponseEntity.ok(orderService.getOrderItems(member, id));
    }
}
