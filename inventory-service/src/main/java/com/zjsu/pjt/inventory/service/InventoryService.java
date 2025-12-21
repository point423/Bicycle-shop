package com.zjsu.pjt.inventory.service;

import com.zjsu.pjt.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import com.zjsu.pjt.inventory.exception.ResourceNotFoundException;
import com.zjsu.pjt.inventory.model.Inventory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map; // 导入 Map
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;


    @Transactional(readOnly = true)
    public Map<UUID, Integer> getStocksByProductIds(List<UUID> productIds) {
        return inventoryRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(Inventory::getProductId, Inventory::getStock));
    }

    @Transactional
    public void deleteInventory(UUID productId) {
        inventoryRepository.deleteByProductId(productId);
    }

    // 更新上架状态的业务方法
    @Transactional
    public void updateOnShelfStatus(UUID productId, boolean onShelf) {
        int updatedRows = inventoryRepository.updateOnShelfStatus(productId, onShelf);
        if (updatedRows == 0) {
            throw new ResourceNotFoundException("找不到商品ID为 " + productId + " 的库存记录，无法更新上架状态。");
        }
    }

    @Transactional
    public void updateStock(UUID productId, Integer newStock) {
        if (newStock < 0) {
            throw new IllegalArgumentException("库存数量不能为负数。");
        }
        int updatedRows = inventoryRepository.updateStockByProductId(productId, newStock);
        if (updatedRows == 0) {
            throw new ResourceNotFoundException("找不到商品ID为 " + productId + " 的库存记录，无法更新库存。");
        }
    }

    /**
     * 根据商品ID获取库存信息。
     *
     * @param productId 商品的UUID
     * @return 对应的库存对象
     * @throws ResourceNotFoundException 如果找不到该商品的库存记录
     */
    @Transactional(readOnly = true) // 这是一个只读操作，标记为 readOnly 可以优化性能
    public Inventory getInventoryByProductId(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product id: " + productId));
    }

    @Transactional
    public Inventory createInventory(UUID productId, Integer initialStock) {
        // 检查该商品的库存记录是否已存在，避免重复创建
        if (inventoryRepository.findByProductId(productId).isPresent()) {
            throw new IllegalStateException("商品 " + productId + " 的库存记录已存在。");
        }
        // 创建新的库存对象
        Inventory newInventory = new Inventory(productId, initialStock);
        // 保存到数据库并返回
        return inventoryRepository.save(newInventory);
    }

    @Transactional
    public void decreaseStock(UUID productId, Integer quantity) {
        int updatedRows = inventoryRepository.decreaseStock(productId, quantity);
        if (updatedRows == 0) {
            // 如果更新行数为0，说明库存不足或商品ID不存在
            throw new RuntimeException("库存不足或商品不存在: " + productId);
        }
    }

    @Transactional
    public void increaseStock(UUID productId, Integer quantity) {
        inventoryRepository.increaseStock(productId, quantity);
    }
}
