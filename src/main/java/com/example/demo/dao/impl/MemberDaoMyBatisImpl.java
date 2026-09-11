package com.example.demo.dao.impl;

import com.example.demo.dao.MemberDao;
import com.example.demo.mapper.MemberMapper;
import com.example.demo.model.Member;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository("memberDaoMyBatis")
public class MemberDaoMyBatisImpl implements MemberDao {

    private final MemberMapper memberMapper;

    public MemberDaoMyBatisImpl(MemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    @Override
    public Member save(Member member) {
        if (member.getId() == null) {
            member.setCreatedAt(LocalDateTime.now());
            memberMapper.insert(member);
        } else {
            memberMapper.update(member);
        }
        return member;
    }

    @Override
    public Optional<Member> findById(Long id) {
        return Optional.ofNullable(memberMapper.findById(id));
    }

    @Override
    public Optional<Member> findByUsername(String username) {
        return Optional.ofNullable(memberMapper.findByUsername(username));
    }

    @Override
    public boolean existsByUsername(String username) {
        return memberMapper.countByUsername(username) > 0;
    }

    @Override
    public List<Member> findAll() {
        return memberMapper.findAll();
    }

    @Override
    public void deleteById(Long id) {
        memberMapper.deleteById(id);
    }

    @Override
    public List<Member> findPage(int page, int size, String keyword) {
        int offset = (page - 1) * size;
        return memberMapper.findPage(offset, size, keyword);
    }

    @Override
    public long count(String keyword) {
        return memberMapper.count(keyword);
    }
}
