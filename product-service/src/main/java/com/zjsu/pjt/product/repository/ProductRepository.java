package com.zjsu.pjt.product.repository;

import com.zjsu.pjt.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 产品数据访问
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    // 根据ID列表和分类进行查询
    List<Product> findByIdInAndCategory(List<UUID> ids, String category);







}