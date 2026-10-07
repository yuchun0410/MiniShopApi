package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.demo.model.Product;

public interface ProductMapper {
	int insert(Product product);
	int deleteById(Long id);
	Product findById(Long id);
	int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);

	List<Product> findPage(@Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);
	long count(@Param("keyword") String keyword);
}
