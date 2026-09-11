package com.example.demo.exception;

// 代表「已經登入，但權限不夠」的情況，交給 GlobalExceptionHandler 轉成 403
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}