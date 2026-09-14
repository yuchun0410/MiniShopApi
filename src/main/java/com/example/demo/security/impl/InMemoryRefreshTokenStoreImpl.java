package com.example.demo.security.impl;

import com.example.demo.security.RefreshTokenStore;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// RefreshTokenStore 的記憶體版實作：後端一重啟資料就全部消失，也沒辦法跨多台伺服器共用。
// 現在已經改用 Redis（見 RedisRefreshTokenStoreImpl），這個版本保留著，
// 之後如果要在沒有 Redis 的環境（例如簡易 demo）跑，隨時可以用 @Qualifier 切回來。
@Component("refreshTokenStoreInMemory")
public class InMemoryRefreshTokenStoreImpl implements RefreshTokenStore {

    private static class Entry {
        final String token;
        final long expireAt;

        Entry(String token, long expireAt) {
            this.token = token;
            this.expireAt = expireAt;
        }
    }

    private final Map<Long, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void save(Long memberId, String refreshToken, long ttlMillis) {
        store.put(memberId, new Entry(refreshToken, System.currentTimeMillis() + ttlMillis));
    }

    @Override
    public boolean isValid(Long memberId, String refreshToken) {
        Entry entry = store.get(memberId);
        if (entry == null) {
            return false;
        }
        if (System.currentTimeMillis() > entry.expireAt) {
            store.remove(memberId);
            return false;
        }
        return entry.token.equals(refreshToken);
    }

    @Override
    public void delete(Long memberId) {
        store.remove(memberId);
    }
}
