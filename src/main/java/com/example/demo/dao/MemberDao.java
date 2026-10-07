package com.example.demo.dao;

import com.example.demo.model.Member;
import java.util.List;
import java.util.Optional;

public interface MemberDao {
    Member save(Member member);
    Optional<Member> findById(Long id);
    Optional<Member> findByUsername(String username);
    boolean existsByUsername(String username);
    void deleteById(Long id);

    // page 從 1 開始算；keyword 可為 null 或空字串，代表不篩選（比對 username 或 name）
    List<Member> findPage(int page, int size, String keyword);
    long count(String keyword);
}
