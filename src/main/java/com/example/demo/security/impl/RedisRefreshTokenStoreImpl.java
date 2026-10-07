package com.example.demo.security.impl;

import com.example.demo.security.RefreshTokenStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

// 用 Redis 存 Refresh Token：登出時真的把它從 Redis 刪掉，之後就不能再拿它換 Access Token。
// key 設定 TTL，過期後 Redis 會自動清掉，不用自己寫排程去清過期資料。
@Component("refreshTokenStoreRedis")
public class RedisRefreshTokenStoreImpl implements RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh_token:";

    private final StringRedisTemplate redisTemplate;

    public RedisRefreshTokenStoreImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(Long memberId, String refreshToken, long ttlMillis) {
        redisTemplate.opsForValue().set(key(memberId), refreshToken, Duration.ofMillis(ttlMillis));
    }

    @Override
    public boolean isValid(Long memberId, String refreshToken) {
        String stored = redisTemplate.opsForValue().get(key(memberId));
        return stored != null && stored.equals(refreshToken);
    }

    @Override
    public void delete(Long memberId) {
        redisTemplate.delete(key(memberId));
    }

    private String key(Long memberId) {
        return KEY_PREFIX + memberId;
    }
}
