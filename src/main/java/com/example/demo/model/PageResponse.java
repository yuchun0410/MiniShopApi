package com.example.demo.model;

import java.util.List;

// 通用的分頁回應格式，不管查哪種資料都可以用（用泛型 T）。
// 前端可以直接拿 content 當列表顯示，用 totalPages / page 做換頁按鈕的邏輯。
public class PageResponse<T> {

    private List<T> content;
    private int page;          // 目前第幾頁（從 1 開始）
    private int size;          // 每頁筆數
    private long totalElements; // 總筆數
    private int totalPages;    // 總頁數

    public PageResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<T> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}
