package com.example.demo.security;

// 集中管理「目前有效的 Refresh Token」，抽成介面是為了跟專案裡 Dao 那套一樣的做法：
// 先用記憶體版本讓整個 JWT 流程能動、能測試，之後裝好 Redis 再換一個實作，
// Service／Controller 完全不用改，只是把底層儲存方式換掉。
public interface RefreshTokenStore {

    // 儲存這個會員目前唯一有效的 refresh token；同一個會員再次登入會直接覆蓋舊的，
    // 等於自動讓舊的 refresh token 失效（一次只允許一組有效的登入狀態）。
    void save(Long memberId, String refreshToken, long ttlMillis);

    // 檢查這組 refresh token 是不是這個會員目前記錄在案、且尚未過期的那一組
    boolean isValid(Long memberId, String refreshToken);

    // 登出時呼叫，把這個會員的 refresh token 記錄整個刪掉
    void delete(Long memberId);
}
