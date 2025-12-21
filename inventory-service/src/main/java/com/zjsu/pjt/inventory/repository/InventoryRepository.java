package com.zjsu.pjt.inventory.repository;

import com.zjsu.pjt.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {


    @Modifying
    @Query("UPDATE Inventory i SET i.stock = :stock WHERE i.productId = :productId")
    int updateStockByProductId(@Param("productId") UUID productId, @Param("stock") Integer stock);

    // 根据商品ID查找库存
    Optional<Inventory> findByProductId(UUID productId);

    List<Inventory> findByProductIdIn(List<UUID> productIds);


    // 查询所有上架状态为true的库存记录
    List<Inventory> findByOnShelfTrue();

    // 【关键】使用原子操作扣减库存，防止超卖
    // 返回值int表示更新的行数。如果为1，表示扣减成功；如果为0，表示库存不足或商品不存在
    @Modifying
    @Query("UPDATE Inventory i SET i.stock = i.stock - :quantity WHERE i.productId = :productId AND i.stock >= :quantity")
    int decreaseStock(UUID productId, Integer quantity);

    // 增加库存（例如取消订单时）
    @Modifying
    @Query("UPDATE Inventory i SET i.stock = i.stock + :quantity WHERE i.productId = :productId")
    void increaseStock(UUID productId, Integer quantity);

    // 根据商品ID删除库存记录
    void deleteByProductId(UUID productId);

    // 根据商品ID更新上架状态
    @Modifying
    @Query("UPDATE Inventory i SET i.onShelf = :onShelf WHERE i.productId = :productId")
    int updateOnShelfStatus(UUID productId, boolean onShelf);
}
