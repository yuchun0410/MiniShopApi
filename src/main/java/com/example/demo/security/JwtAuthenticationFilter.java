package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 攔截每個請求，檢查 Authorization header 裡有沒有帶著有效的 JWT Access Token。
// 驗證通過就把 memberId／role 塞進 request 屬性，讓 Controller 可以像以前用
// session.getAttribute("memberId") 一樣，改用 request.getAttribute("memberId") 取得目前登入者，
// 這樣每個 Controller 需要改的地方最小，不用整套換成 Spring Security 的 Authentication 物件。
//
// 驗證失敗或沒帶 token，這裡不會直接擋下請求——維持跟原本 Session 機制一樣的行為，
// 交給各個 Controller 自己判斷 memberId 是不是 null，null 就丟例外讓 GlobalExceptionHandler
// 轉成 401，這樣公開端點（像商品列表）不用帶 token 也能正常訪問。
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.parseClaims(token);
                // Refresh Token 不能拿來當一般 API 的身份憑證用，只能拿去打 /refresh 換新的
                // Access Token，這裡特別擋掉，避免有人拿效期很長的 refresh token 冒充 access token。
                if (!jwtUtil.isRefreshToken(claims)) {
                    request.setAttribute("memberId", jwtUtil.extractMemberId(claims));
                    request.setAttribute("role", jwtUtil.extractRole(claims));
                }
            } catch (JwtException | IllegalArgumentException e) {
                // token 過期、簽章不對、格式錯誤都會進到這裡：不設定 memberId 屬性，
                // 讓請求繼續往下走，交給 Controller 判斷「沒登入」該怎麼回應。
            }
        }

        filterChain.doFilter(request, response);
    }
}
