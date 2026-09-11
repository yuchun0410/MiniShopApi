package com.example.demo.dao.impl;

import com.example.demo.dao.MemberDao;
import com.example.demo.model.Member;
import com.example.demo.repository.MemberRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

// ProductDao 的 JPA 版本一樣，這是 MemberDao 的 JPA 版本實作，示範 @Qualifier 用法。
// 注意 save() 這裡不用像 MyBatis 版那樣手動判斷 insert/update、手動設定 createdAt，
// 因為 Member 這個 entity 本來就有 @PrePersist，透過 JpaRepository.save() 會自動觸發，
// 而且 save() 本身在 JPA 就同時處理新增（id 為 null）跟更新（id 不為 null）兩種情況。
@Repository("memberDaoJpa")
public class MemberDaoJpaImpl implements MemberDao {

    private final MemberRepository memberRepository;

    public MemberDaoJpaImpl(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public Member save(Member member) {
        return memberRepository.save(member);
    }

    @Override
    public Optional<Member> findById(Long id) {
        return memberRepository.findById(id);
    }

    @Override
    public Optional<Member> findByUsername(String username) {
        return memberRepository.findByUsername(username);
    }

    @Override
    public boolean existsByUsername(String username) {
        return memberRepository.existsByUsername(username);
    }

    @Override
    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        memberRepository.deleteById(id);
    }

    @Override
    public List<Member> findPage(int page, int size, String keyword) {
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        if (StringUtils.hasText(keyword)) {
            return memberRepository.findByUsernameContainingOrNameContaining(keyword, keyword, pageRequest).getContent();
        }
        return memberRepository.findAll(pageRequest).getContent();
    }

    @Override
    public long count(String keyword) {
        if (StringUtils.hasText(keyword)) {
            return memberRepository.countByUsernameContainingOrNameContaining(keyword, keyword);
        }
        return memberRepository.count();
    }
}
