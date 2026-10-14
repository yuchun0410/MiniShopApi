package com.example.demo.controller;

import com.example.demo.model.Member;
import com.example.demo.model.Role;
import com.example.demo.security.JwtUtil;
import com.example.demo.security.RefreshTokenStore;
import com.example.demo.service.MemberService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @Mock MemberService memberService;
    @Mock JwtUtil jwtUtil;
    @Mock RefreshTokenStore refreshTokenStore;

    MemberController controller;
    Member member;

    @BeforeEach
    void setUp() {
        controller = new MemberController(memberService, jwtUtil, refreshTokenStore);
        // @Value 欄位在單元測試裡不會被 Spring 注入，要自己塞
        ReflectionTestUtils.setField(controller, "refreshTokenExpirationMs", 7L * 24 * 60 * 60 * 1000);
        ReflectionTestUtils.setField(controller, "cookieSecure", true);

        member = new Member();
        member.setId(42L);
        member.setRole(Role.MEMBER);
    }

    @Test
    void login_shouldSetRefreshTokenInHttpOnlyCookieAndReturnAccessTokenInBody() {
        when(memberService.login("user", "pw")).thenReturn(member);
        when(jwtUtil.generateAccessToken(member)).thenReturn("ACCESS");
        when(jwtUtil.generateRefreshToken(member)).thenReturn("REFRESH");

        MemberController.LoginRequest req = new MemberController.LoginRequest();
        req.username = "user";
        req.password = "pw";

        ResponseEntity<MemberController.LoginResponse> res = controller.login(req);

        assertEquals("ACCESS", res.getBody().accessToken);
        String cookie = res.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(cookie);
        assertTrue(cookie.contains("refreshToken=REFRESH"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertTrue(cookie.contains("Path=/api/members"));
        verify(refreshTokenStore).save(eq(42L), eq("REFRESH"), anyLong());
    }

    @Test
    void refresh_withValidCookie_shouldReturnNewAccessToken() {
        Claims claims = mock(Claims.class);
        when(jwtUtil.parseClaims("REFRESH")).thenReturn(claims);
        when(jwtUtil.isRefreshToken(claims)).thenReturn(true);
        when(jwtUtil.extractMemberId(claims)).thenReturn(42L);
        when(refreshTokenStore.isValid(42L, "REFRESH")).thenReturn(true);
        when(memberService.findById(42L)).thenReturn(member);
        when(jwtUtil.generateAccessToken(member)).thenReturn("NEW_ACCESS");

        ResponseEntity<MemberController.RefreshResponse> res = controller.refresh("REFRESH");

        assertEquals("NEW_ACCESS", res.getBody().accessToken);
    }

    @Test
    void refresh_withoutCookie_shouldThrowAndNotTouchTokenOrRedis() {
        assertThrows(IllegalStateException.class, () -> controller.refresh(null));

        verifyNoInteractions(jwtUtil, refreshTokenStore);
    }

    @Test
    void logout_shouldDeleteStoredTokenAndClearCookie() {
        Claims claims = mock(Claims.class);
        when(jwtUtil.parseClaims("REFRESH")).thenReturn(claims);
        when(jwtUtil.extractMemberId(claims)).thenReturn(42L);

        ResponseEntity<Void> res = controller.logout("REFRESH");

        verify(refreshTokenStore).delete(42L);
        String cookie = res.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(cookie);
        assertTrue(cookie.contains("Max-Age=0"));
        assertTrue(cookie.contains("Path=/api/members"));
    }
}