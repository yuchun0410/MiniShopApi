package com.example.demo.controller;

import com.example.demo.model.Member;
import com.example.demo.model.MemberResponse;
import com.example.demo.model.PageResponse;
import com.example.demo.model.Role;
import com.example.demo.security.JwtUtil;
import com.example.demo.security.RefreshTokenStore;
import com.example.demo.service.MemberService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import java.time.Duration;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private static final Logger log = LoggerFactory.getLogger(MemberController.class);

    private final MemberService memberService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenStore refreshTokenStore;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;
    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    // Refresh Token 已經改存 Redis，明確用 @Qualifier 指定，避免 Spring 因為有兩個
    // RefreshTokenStore 實作（Redis 版、記憶體版）而丟出 NoUniqueBeanDefinitionException。
    public MemberController(MemberService memberService, JwtUtil jwtUtil,
                             @Qualifier("refreshTokenStoreRedis") RefreshTokenStore refreshTokenStore) {
        this.memberService = memberService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenStore = refreshTokenStore;
    }

    @PostMapping("/register")
    public ResponseEntity<MemberResponse> register(@RequestBody RegisterRequest req) {
        Member member = memberService.register(req.username, req.password, req.email, req.name);
        log.info("會員註冊成功，memberId={}, username={}", member.getId(), member.getUsername());
        return ResponseEntity.ok(MemberResponse.from(member));
    }

    // 登入：驗證帳密後簽發 Access Token（短效期）與 Refresh Token（長效期）
    // Refresh Token 另外存進 RefreshTokenStore，之後 /refresh、/logout 都要用它比對
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        Member member = memberService.login(req.username, req.password);
        String accessToken = jwtUtil.generateAccessToken(member);
        String refreshToken = jwtUtil.generateRefreshToken(member);
        refreshTokenStore.save(member.getId(), refreshToken, refreshTokenExpirationMs);
        log.info("會員登入成功，memberId={}, username={}", member.getId(), member.getUsername());
        ResponseCookie cookie = buildRefreshCookie(refreshToken, refreshTokenExpirationMs);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new LoginResponse(MemberResponse.from(member), accessToken));
    }

    // 登出：把 Refresh Token 從 RefreshTokenStore 刪掉，之後就不能再拿它換新的 Access Token
    // 目前的 Access Token 沒辦法立刻作廢，但它效期只有 15 分鐘，風險視窗很小
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken != null) {
            try {
                Claims claims = jwtUtil.parseClaims(refreshToken);
                Long memberId = jwtUtil.extractMemberId(claims);
                refreshTokenStore.delete(memberId);
                log.info("會員登出成功，memberId={}", memberId);
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("登出時的 Refresh Token 已經無效或過期，視同登出成功");
            }
        }
        // 回一個 Max-Age=0 的同名 cookie，叫瀏覽器把它刪掉
        ResponseCookie clearCookie = buildRefreshCookie("", 0);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken == null) {
            log.warn("換發 Access Token 失敗，請求沒有帶 Refresh Token cookie");
            throw new IllegalStateException("尚未登入或登入已過期，請重新登入");
        }
        Claims claims;
        try {
            claims = jwtUtil.parseClaims(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("換發 Access Token 失敗，Refresh Token 無效或已過期");
            throw new IllegalStateException("Refresh Token 無效或已過期，請重新登入");
        }
        if (!jwtUtil.isRefreshToken(claims)) {
            log.warn("換發 Access Token 失敗，傳入的不是 Refresh Token");
            throw new IllegalStateException("這不是 Refresh Token");
        }
        Long memberId = jwtUtil.extractMemberId(claims);
        if (!refreshTokenStore.isValid(memberId, refreshToken)) {
            log.warn("換發 Access Token 失敗，Refresh Token 已被登出或不存在，memberId={}", memberId);
            throw new IllegalStateException("Refresh Token 已被登出或不存在，請重新登入");
        }
        Member member = memberService.findById(memberId);
        String newAccessToken = jwtUtil.generateAccessToken(member);
        log.info("換發 Access Token 成功，memberId={}", memberId);
        return ResponseEntity.ok(new RefreshResponse(newAccessToken));
    }
    private ResponseCookie buildRefreshCookie(String token, long maxAgeMs) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/api/members")
                .maxAge(Duration.ofMillis(maxAgeMs))
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(MemberResponse.from(memberService.findById(memberId)));
    }

    // 會員列表：只有管理員能看，支援換頁 + 查詢（比對 username 或 name）
    // GET /api/members?page=1&size=10&keyword=xxx
    @GetMapping
    public ResponseEntity<PageResponse<MemberResponse>> listAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        requireAdmin(request);
        PageResponse<Member> result = memberService.findPage(page, size, keyword);
        // 每一筆 Member 轉成 MemberResponse 再回傳，分頁資訊照舊
        PageResponse<MemberResponse> response = new PageResponse<>(
                result.getContent().stream().map(MemberResponse::from).toList(),
                result.getPage(), result.getSize(), result.getTotalElements(), result.getTotalPages());
        return ResponseEntity.ok(response);
    }

    // 修改會員角色：只有管理員能操作
    // 呼叫方式：PUT /api/members/3/role?role=ADMIN
    @PutMapping("/{id}/role")
    public ResponseEntity<MemberResponse> updateRole(@PathVariable Long id, @RequestParam Role role, HttpServletRequest request) {
        requireAdmin(request);
        Member updated = memberService.updateRole(id, role);
        log.info("修改會員角色成功，memberId={}, newRole={}", id, role);
        return ResponseEntity.ok(MemberResponse.from(updated));
    }

    // 刪除會員：只有管理員能操作
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id, HttpServletRequest request) {
        requireAdmin(request);
        memberService.deleteMember(id);
        log.info("刪除會員成功，memberId={}", id);
        return ResponseEntity.ok().build();
    }

    // 共用檢查：沒登入 -> 丟例外讓 GlobalExceptionHandler 轉成 401；不是管理員 -> 轉成 403
    private void requireAdmin(HttpServletRequest request) {
        Long memberId = (Long) request.getAttribute("memberId");
        if (memberId == null) {
            log.warn("未登入狀態嘗試存取會員管理 API，uri={}", request.getRequestURI());
            throw new IllegalStateException("尚未登入");
        }
        Member current = memberService.findById(memberId);
        if (current.getRole() != com.example.demo.model.Role.ADMIN) {
            log.warn("非管理員嘗試存取會員管理 API，memberId={}, uri={}", memberId, request.getRequestURI());
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
        public MemberResponse member;
        public String accessToken;

        public LoginResponse(MemberResponse member, String accessToken) {
            this.member = member;
            this.accessToken = accessToken;
        }
    }



    public static class RefreshResponse {
        public String accessToken;

        public RefreshResponse(String accessToken) {
            this.accessToken = accessToken;
        }
    }


}
