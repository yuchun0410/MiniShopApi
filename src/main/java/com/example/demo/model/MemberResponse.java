package com.example.demo.model;

import java.time.LocalDateTime;

// 回傳給前端的會員資料（DTO）。
// 只放「明確允許對外」的欄位（白名單）——password 根本不在這個類別裡，
// 所以不管 Member Entity 以後新增什麼欄位，都不會自動跑到 API 回應裡。
// 欄位名稱和原本直接回傳 Member 時的 JSON 一致，前端不用跟著改。
public class MemberResponse {

    private final Long id;
    private final String username;
    private final String email;
    private final String name;
    private final Role role;
    private final LocalDateTime createdAt;

    private MemberResponse(Member member) {
        this.id = member.getId();
        this.username = member.getUsername();
        this.email = member.getEmail();
        this.name = member.getName();
        this.role = member.getRole();
        this.createdAt = member.getCreatedAt();
    }

    // Entity -> DTO 的轉換集中在這一個地方
    public static MemberResponse from(Member member) {
        return new MemberResponse(member);
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
