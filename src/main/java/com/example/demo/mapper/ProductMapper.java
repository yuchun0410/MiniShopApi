package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.demo.model.Product;

public interface ProductMapper {
	List<Product> findAll();
	Product findById(Long id);

	List<Product> findPage(@Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);
	long count(@Param("keyword") String keyword);
}
