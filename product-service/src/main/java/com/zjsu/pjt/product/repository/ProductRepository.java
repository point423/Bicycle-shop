package com.zjsu.pjt.product.repository;

import com.zjsu.pjt.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page; // 导入 Page
import org.springframework.data.domain.Pageable; // 导入 Pageable

/**
 * 产品数据访问
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    // 根据ID列表和分类进行查询
    List<Product> findByIdInAndCategory(List<UUID> ids, String category);

    /**
     * 根据提供的ID列表，进行分页查询
     * @param ids 商品ID的列表
     * @param pageable 分页信息对象
     * @return 包含商品的分页结果
     */
    Page<Product> findByIdIn(List<UUID> ids, Pageable pageable);


    Page<Product> findByIdInAndCategory(List<UUID> ids, String category, Pageable pageable);
}