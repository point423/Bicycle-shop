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
    // 按品牌查询所有已上架的商品
    List<Product> findByBrandAndOnShelfTrue(String brand);

    // 按分类查询所有已上架的商品
    List<Product> findByCategoryAndOnShelfTrue(String category);

    // 查询所有已上架的商品
    List<Product> findByOnShelfTrue();







}