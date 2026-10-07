package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.demo.model.Member;

public interface MemberMapper {
	int insert(Member member);
	int update(Member member);
	Member findById(Long id);
	Member findByUsername(String username);
	int countByUsername(String username);
	int deleteById(Long id);

	List<Member> findPage(@Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);
	long count(@Param("keyword") String keyword);
}
