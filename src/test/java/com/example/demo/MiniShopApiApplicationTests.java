package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.mapper.MemberMapper;
import com.example.demo.model.Member;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MemberMapperTest {

    @Autowired
    private MemberMapper memberMapper;  // 把 MemberMapper 注入進來

    @Test
    void testFindByUsername() {
        Member member = memberMapper.findByUsername("admin");  // 呼叫哪個方法？

        assertNotNull(member,"查無此會員資料");           // 驗證有查到資料
        assertEquals("admin", member.getUsername());   // 驗證 username 真的是 admin
    }
}