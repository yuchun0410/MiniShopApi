package com.example.demo.controller;

import com.example.demo.model.Member;
import com.example.demo.model.PageResponse;
import com.example.demo.model.Role;
import com.example.demo.service.MemberService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("/register")
    public ResponseEntity<Member> register(@RequestBody RegisterRequest req) {
        Member member = memberService.register(req.username, req.password, req.email, req.name);
        return ResponseEntity.ok(member);
    }

    @PostMapping("/login")
    public ResponseEntity<Member> login(@RequestBody LoginRequest req, HttpSession session) {
        Member member = memberService.login(req.username, req.password);
        session.setAttribute("memberId", member.getId());
        return ResponseEntity.ok(member);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<Member> me(HttpSession session) {
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(memberService.findById(memberId));
    }

    // 會員列表：只有管理員能看，支援換頁 + 查詢（比對 username 或 name）
    // GET /api/members?page=1&size=10&keyword=xxx
    @GetMapping
    public ResponseEntity<PageResponse<Member>> listAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            HttpSession session) {
        requireAdmin(session);
        return ResponseEntity.ok(memberService.findPage(page, size, keyword));
    }

    // 修改會員角色：只有管理員能操作
    // 呼叫方式：PUT /api/members/3/role?role=ADMIN
    @PutMapping("/{id}/role")
    public ResponseEntity<Member> updateRole(@PathVariable Long id, @RequestParam Role role, HttpSession session) {
        requireAdmin(session);
        return ResponseEntity.ok(memberService.updateRole(id, role));
    }

    // 刪除會員：只有管理員能操作
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id, HttpSession session) {
        requireAdmin(session);
        memberService.deleteMember(id);
        return ResponseEntity.ok().build();
    }

    // 共用檢查：沒登入 -> 丟例外讓 GlobalExceptionHandler 轉成 401；不是管理員 -> 轉成 403
    private void requireAdmin(HttpSession session) {
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) {
            throw new IllegalStateException("尚未登入");
        }
        Member current = memberService.findById(memberId);
        if (current.getRole() != com.example.demo.model.Role.ADMIN) {
            throw new com.example.demo.exception.AccessDeniedException("需要管理員權限");
        }
    }

    public static class RegisterRequest {
        public String username;
        public String password;
        public String email;
        public String name;
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }
}