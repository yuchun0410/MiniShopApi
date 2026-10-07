package com.example.demo.service.impl;

import com.example.demo.dao.MemberDao;
import org.springframework.beans.factory.annotation.Qualifier;
import com.example.demo.model.Member;
import com.example.demo.model.PageResponse;
import com.example.demo.model.Role;
import com.example.demo.service.MemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberServiceImpl implements MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberServiceImpl.class);

    private final MemberDao memberDao;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public MemberServiceImpl(@Qualifier("memberDaoMyBatis") MemberDao memberDao) {
        this.memberDao = memberDao;
    }

    @Override
    public Member register(String username, String rawPassword, String email, String name) {
        if (memberDao.existsByUsername(username)) {
            log.warn("註冊失敗，帳號已存在: {}", username);
            throw new IllegalArgumentException("此帳號已被註冊");
        }

        Member member = new Member();
        member.setUsername(username);
        member.setPassword(passwordEncoder.encode(rawPassword));
        member.setEmail(email);
        member.setName(name);

        Member saved = memberDao.save(member);
        log.info("新會員註冊成功，id={}, username={}", saved.getId(), username);
        return saved;
    }

    @Override
    public Member login(String username, String rawPassword) {
        Member member = memberDao.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("登入失敗，找不到帳號: {}", username);
                    return new IllegalArgumentException("帳號或密碼錯誤");
                });

        if (!passwordEncoder.matches(rawPassword, member.getPassword())) {
            log.warn("登入失敗，密碼錯誤: {}", username);
            throw new IllegalArgumentException("帳號或密碼錯誤");
        }

        log.info("會員登入成功: {}", username);
        return member;
    }

    @Override
    public Member findById(Long id) {
        return memberDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("找不到此會員"));
    }

    @Override
    public PageResponse<Member> findPage(int page, int size, String keyword) {
        List<Member> content = memberDao.findPage(page, size, keyword);
        long totalElements = memberDao.count(keyword);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }

    @Override
    public Member updateRole(Long memberId, Role newRole) {
        Member member = memberDao.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("找不到此會員"));
        member.setRole(newRole);
        Member saved = memberDao.save(member);
        log.info("修改會員角色，memberId={}, newRole={}", memberId, newRole);
        return saved;
    }

    @Override
    public void deleteMember(Long memberId) {
        if (memberDao.findById(memberId).isEmpty()) {
            throw new IllegalArgumentException("找不到此會員");
        }
        memberDao.deleteById(memberId);
        log.info("刪除會員，memberId={}", memberId);
    }
}