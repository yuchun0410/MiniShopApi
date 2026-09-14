package com.example.demo.controller;

import com.example.demo.model.Member;
import com.example.demo.model.PageResponse;
import com.example.demo.model.Role;
import com.example.demo.security.JwtUtil;
import com.example.demo.security.RefreshTokenStore;
import com.example.demo.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenStore refreshTokenStore;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    // Refresh Token 已經改存 Redis，明確用 @Qualifier 指定，避免 Spring 因為有兩個
    // RefreshTokenStore 實作（Redis 版、記憶體版）而丟出 NoUniqueBeanDefinitionException。
    public MemberController(MemberService memberService, JwtUtil jwtUtil,
                             @Qualifier("refreshTokenStoreRedis") RefreshTokenStore refreshTokenStore) {
        this.memberService = memberService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenStore = refreshTokenStore;
    }

    @PostMapping("/register")
    public ResponseEntity<Member> register(@RequestBody RegisterRequest req) {
        Member member = memberService.register(req.username, req.password, req.email, req.name);
        return ResponseEntity.ok(member);
    }

    // 登入：驗證帳密後簽發 Access Token（短效期）與 Refresh Token（長效期）
    // Refresh Token 另外存進 RefreshTokenStore，之後 /refresh、/logout 都要用它比對
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        Member member = memberService.login(req.username, req.password);
        String accessToken = jwtUtil.generateAccessToken(member);
        String refreshToken = jwtUtil.generateRefreshToken(member);
        refreshTokenStore.save(member.getId(), refreshToken, refreshTokenExpirationMs);
        return ResponseEntity.ok(new LoginResponse(member, accessToken, refreshToken));
    }

    // 登出：把 Refresh Token 從 RefreshTokenStore 刪掉，之後就不能再拿它換新的 Access Token
    // 目前的 Access Token 沒辦法立刻作廢，但它效期只有 15 分鐘，風險視窗很小
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) LogoutRequest req) {
        if (req != null && req.refreshToken != null) {
            try {
                Claims claims = jwtUtil.parseClaims(req.refreshToken);
                Long memberId = jwtUtil.extractMemberId(claims);
                refreshTokenStore.delete(memberId);
            } catch (JwtException | IllegalArgumentException e) {
                // token 本來就無效或已過期，視同登出成功，不用特別處理
            }
        }
        return ResponseEntity.ok().build();
    }

    // 用 Refresh Token 換一組新的 Access Token（沒有做 Refresh Token 輪替，簡化版）
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@RequestBody RefreshRequest req) {
        Claims claims;
        try {
            claims = jwtUtil.parseClaims(req.refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalStateException("Refresh Token 無效或已過期，請重新登入");
        }
        if (!jwtUtil.isRefreshToken(claims)) {
            throw new IllegalStateException("這不是 Refresh Token");
        }
        Long memberId = jwtUtil.extractMemberId(claims);
        if (!refreshTokenStore.isValid(memberId, req.refreshToken)) {
            throw new IllegalStateException("Refresh Token 已被登出或不存在，請重新登入");
        }
        Member member = memberService.findById(memberId);
        String newAccessToken = jwtUtil.generateAccessToken(member);
        return ResponseEntity.ok(new RefreshResponse(newAccessToken));
    }

    @GetMapping("/me")
    public ResponseEntity<Member> me(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
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
            HttpServletRequest request) {
        requireAdmin(request);
        return ResponseEntity.ok(memberService.findPage(page, size, keyword));
    }

    // 修改會員角色：只有管理員能操作
    // 呼叫方式：PUT /api/members/3/role?role=ADMIN
    @PutMapping("/{id}/role")
    public ResponseEntity<Member> updateRole(@PathVariable Long id, @RequestParam Role role, HttpServletRequest request) {
        requireAdmin(request);
        return ResponseEntity.ok(memberService.updateRole(id, role));
    }

    // 刪除會員：只有管理員能操作
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        memberService.deleteMember(id);
        return ResponseEntity.ok().build();
    }

    // 共用檢查：沒登入 -> 丟例外讓 GlobalExceptionHandler 轉成 401；不是管理員 -> 轉成 403
    private void requireAdmin(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
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

    public static class LoginResponse {
        public Member member;
        public String accessToken;
        public String refreshToken;

        public LoginResponse(Member member, String accessToken, String refreshToken) {
            this.member = member;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    public static class RefreshRequest {
        public String refreshToken;
    }

    public static class RefreshResponse {
        public String accessToken;

        public RefreshResponse(String accessToken) {
            this.accessToken = accessToken;
        }
    }

    public static class LogoutRequest {
        public String refreshToken;
    }
}
