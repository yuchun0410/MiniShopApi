package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
	// 登入、檢查帳號是否重複時會用到:依 username 查 Member
	Optional<Member> findByUsername(String username);

    boolean existsByUsername(String username);

    // 換頁 + 查詢：username 或 name 包含 keyword 的分頁結果
    Page<Member> findByUsernameContainingOrNameContaining(String usernameKeyword, String nameKeyword, Pageable pageable);

    long countByUsernameContainingOrNameContaining(String usernameKeyword, String nameKeyword);
}
