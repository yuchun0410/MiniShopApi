package com.example.demo.service;

import com.example.demo.model.Member;
import com.example.demo.model.PageResponse;
import com.example.demo.model.Role;
import java.util.List;

public interface MemberService {
    Member register(String username, String rawPassword, String email, String name);
    Member login(String username, String rawPassword);
    Member findById(Long id);
    List<Member> findAll();
    PageResponse<Member> findPage(int page, int size, String keyword);
    Member updateRole(Long memberId, Role newRole);
    void deleteMember(Long memberId);
}
