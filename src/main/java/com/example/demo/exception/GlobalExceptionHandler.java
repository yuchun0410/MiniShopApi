package com.example.demo.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 集中攔截 Service 層丟出來的業務邏輯例外，轉成乾淨的錯誤回應
// 沒有這個的話，這些例外會被當成系統錯誤，統一回傳 500
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 帳密錯誤、找不到資料、重複註冊等 -> 400（請求本身有問題）
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    // 尚未登入就操作購物車等 -> 401（未驗證）
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(401).body(e.getMessage());
    }
    // 已登入但權限不夠（例如不是管理員）-> 403（禁止）
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(403).body(e.getMessage());
    }
    // 資料庫外鍵限制擋下來的刪除/更新（例如會員底下還有訂單、商品還在購物車裡）-> 400，回友善訊息
    // 訊息故意寫通用一點，因為這個 handler 是共用的，不是只有會員刪除會觸發
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException e) {
        return ResponseEntity.badRequest().body("此筆資料仍被其他資料參照，無法刪除");
    }
}