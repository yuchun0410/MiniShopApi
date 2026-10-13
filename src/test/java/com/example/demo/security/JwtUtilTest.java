package com.example.demo.security;

import com.example.demo.model.Member;
import com.example.demo.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 測 JwtUtil：私鑰簽發、公鑰驗證。
 * 測試裡自己即時產生一組 RSA 金鑰，不需要任何環境變數，也不會用到真正的金鑰。
 */
class JwtUtilTest {

    private static final long ONE_HOUR_MS = 60 * 60 * 1000L;

    private JwtUtil jwtUtil;
    private Member admin;
    private Member normalMember;

    private static KeyPair newKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    // 私鑰（PKCS#8）與公鑰（X.509）轉成 Base64 字串，格式跟 JwtUtil 建構子要的一樣
    private static JwtUtil newJwtUtil(KeyPair keyPair, long accessMs, long refreshMs) {
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        return new JwtUtil(privateKey, publicKey, accessMs, refreshMs);
    }

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = newJwtUtil(newKeyPair(), ONE_HOUR_MS, ONE_HOUR_MS);

        admin = new Member();
        admin.setId(42L);
        admin.setRole(Role.ADMIN);

        normalMember = new Member();
        normalMember.setId(7L);
        normalMember.setRole(Role.MEMBER);
    }

    @Test
    void accessToken_shouldCarryMemberIdAndRole() {
        String token = jwtUtil.generateAccessToken(admin);

        Claims claims = jwtUtil.parseClaims(token);

        assertEquals(42L, jwtUtil.extractMemberId(claims));
        assertEquals("ADMIN", jwtUtil.extractRole(claims));
        assertFalse(jwtUtil.isRefreshToken(claims));
    }

    @Test
    void refreshToken_shouldBeRecognizedAsRefresh() {
        String token = jwtUtil.generateRefreshToken(admin);

        Claims claims = jwtUtil.parseClaims(token);

        assertTrue(jwtUtil.isRefreshToken(claims));
    }

    @Test
    void expiredToken_shouldBeRejected() throws Exception {
        // 有效時間設成負數 = 簽發出來就已經過期
        JwtUtil expiredUtil = newJwtUtil(newKeyPair(), -1000L, -1000L);
        String token = expiredUtil.generateAccessToken(admin);

        assertThrows(ExpiredJwtException.class, () -> expiredUtil.parseClaims(token));
    }

    @Test
    void tokenSignedByAnotherPrivateKey_shouldBeRejected() throws Exception {
        // 攻擊者用「自己的私鑰」簽出一張看似合法的 token
        JwtUtil attacker = newJwtUtil(newKeyPair(), ONE_HOUR_MS, ONE_HOUR_MS);
        String forged = attacker.generateAccessToken(admin);

        // 伺服器用「自己的公鑰」驗簽，簽章對不上，一定被拒絕
        assertThrows(JwtException.class, () -> jwtUtil.parseClaims(forged));
    }

    @Test
    void tamperedPayload_shouldBeRejected() {
        // 一般會員的 token：header.payload.signature
        String[] memberParts = jwtUtil.generateAccessToken(normalMember).split("\\.");
        // 管理員的 token，只拿它的 payload（裡面 role = ADMIN）
        String[] adminParts = jwtUtil.generateAccessToken(admin).split("\\.");

        // 模擬「想把自己的 role 改成 ADMIN」：換上管理員的 payload，但簽章還是原本一般會員的
        String tampered = memberParts[0] + "." + adminParts[1] + "." + memberParts[2];

        // payload 一被動過，簽章就對不上，所以改 role 提權會被發現
        assertThrows(JwtException.class, () -> jwtUtil.parseClaims(tampered));
    }
}
