package com.zjsu.pjt.inventory.controller;

import com.zjsu.pjt.inventory.dto.InventoryUpdateRequest;
import com.zjsu.pjt.inventory.service.InventoryService;
import com.zjsu.pjt.inventory.model.Inventory;
import com.zjsu.pjt.inventory.dto.InventoryCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.zjsu.pjt.inventory.repository.InventoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;


    @Autowired
    private InventoryRepository inventoryRepository;

    // 获取所有已上架商品的ID列表
    @GetMapping("/on-shelf-product-ids")
    @Operation(summary = "获取所有已上架商品的Product ID列表")
    public List<UUID> getOnShelfProductIds() {
        return inventoryRepository.findByOnShelfTrue().stream()
                .map(Inventory::getProductId) // 从Inventory对象中提取productId字符串
                .collect(Collectors.toList());
    }

    // 用于更新上架状态的内部接口
    @PutMapping("/{productId}/on-shelf")
    public ResponseEntity<Void> updateOnShelfStatus(
            @PathVariable UUID productId,
            @RequestParam boolean onShelf) {
        inventoryService.updateOnShelfStatus(productId, onShelf);
        return ResponseEntity.ok().build();
    }

    //创建库存实例
    @PostMapping("/create")
    public ResponseEntity<Inventory> createInventoryRecord(@RequestBody InventoryCreateRequest request) {
        try {
            // 调用service层创建库存记录
            Inventory createdInventory = inventoryService.createInventory(
                    request.getProductId(),
                    request.getStock()
            );
            // 返回 201 Created 状态码和创建成功的库存对象
            return new ResponseEntity<>(createdInventory, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            // 如果记录已存在，返回 409 Conflict 状态码
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            // 其他未知错误，返回 500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 根据商品ID查询其库存详情。
     *
     * @param productId 商品的UUID，从URL路径中获取
     * @return 返回包含库存信息的ResponseEntity
     */
    @GetMapping("/{productId}")
    @Operation(summary = "根据商品ID获取库存详情")
    public ResponseEntity<Inventory> getInventoryByProductId(@PathVariable UUID productId) {
        Inventory inventory = inventoryService.getInventoryByProductId(productId);
        return ResponseEntity.ok(inventory);
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "根据商品ID删除库存记录")
    public ResponseEntity<Void> deleteInventoryRecord(@PathVariable UUID productId) {
        inventoryService.deleteInventory(productId);
        return ResponseEntity.noContent().build(); // 返回 204 No Content
    }


    // 扣减库存的接口
    @PostMapping("/decrease")
    public ResponseEntity<Void> decreaseStock(@RequestBody InventoryUpdateRequest request) {
        try {
            inventoryService.decreaseStock(request.getProductId(), request.getQuantity());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // 返回400 Bad Request表示客户端请求有问题（比如库存不足）
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // 增加库存的接口
    @PostMapping("/increase")
    public ResponseEntity<Void> increaseStock(@RequestBody InventoryUpdateRequest request) {
        inventoryService.increaseStock(request.getProductId(), request.getQuantity());
        return ResponseEntity.ok().build();
    }
}
