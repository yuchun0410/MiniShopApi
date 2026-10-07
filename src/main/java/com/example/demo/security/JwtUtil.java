package com.example.demo.security;

import com.example.demo.model.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

// 改用 RSA（非對稱式）簽章：private key 簽發 token，public key 驗證 token。
// 跟之前 HMAC（對稱式）版本比，多服務架構下可以只把 public key 分給要驗證 token 的其他服務，
// 不用把能簽發 token 的 private key 也一起交出去；單一服務其實兩種做法差異不大，這裡純粹示範。
@Component
public class JwtUtil {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtUtil(@Value("${app.jwt.private-key}") String privateKeyBase64,
                   @Value("${app.jwt.public-key}") String publicKeyBase64,
                   @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
                   @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
            this.privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            this.publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // 金鑰格式不對或演算法不支援，這種問題只能在啟動時就讓它爆掉，不應該讓服務帶著壞掉的金鑰跑起來
            throw new IllegalStateException("JWT RSA 金鑰載入失敗，請確認 app.jwt.private-key / app.jwt.public-key 設定", e);
        }

        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(Member member) {
        return buildToken(member, accessTokenExpirationMs, "access");
    }

    public String generateRefreshToken(Member member) {
        return buildToken(member, refreshTokenExpirationMs, "refresh");
    }

    private String buildToken(Member member, long expirationMs, String type) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(String.valueOf(member.getId()))
                .claim("role", member.getRole().name())
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(privateKey, Jwts.SIG.RS256)//RSA私鑰
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)//公鑰驗證簽章
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractMemberId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }
}
