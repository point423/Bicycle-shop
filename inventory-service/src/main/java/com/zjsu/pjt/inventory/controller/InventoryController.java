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
import org.springframework.core.env.Environment;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/inventorys")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @Autowired
    private Environment environment;

    @Autowired
    private InventoryRepository inventoryRepository;

    // 获取所有已上架商品的ID列表
    @GetMapping("/on-shelf-product-ids")
    @Operation(summary = "获取所有已上架商品的Product ID列表")
    public ResponseEntity<Map<String, Object>> getOnShelfProductIds() { // 1. 修改返回类型为 ResponseEntity<Map<...>>
        // 2. 获取真实的 ID 列表
        List<UUID> idList = inventoryRepository.findByOnShelfTrue().stream()
                .map(Inventory::getProductId)
                .collect(Collectors.toList());

        // 3. 获取当前服务的端口号
        String port = environment.getProperty("local.server.port");

        // 4. 构建包含 data 和 port 的响应体
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("data", idList); // 将 ID 列表放入 "data" 字段
        responseBody.put("port", port); // 将端口信息放入 "port" 字段

        // 5. 返回包装后的 Map
        return ResponseEntity.ok(responseBody);
    }

    // 用于更新上架状态的内部接口
    @PutMapping("/{productId}/on-shelf")
    public ResponseEntity<Map<String, Object>> updateOnShelfStatus( // 1. 修改返回类型
                                                                    @PathVariable UUID productId,
                                                                    @RequestParam boolean onShelf) {
        // 2. 执行业务逻辑
        inventoryService.updateOnShelfStatus(productId, onShelf);

        // 3. 获取端口号并构建响应体
        String port = environment.getProperty("local.server.port");
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", "Status updated successfully");
        responseBody.put("port",port);

        // 4. 返回包含端口信息的 200 OK 响应
        return ResponseEntity.ok(responseBody);
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
    public ResponseEntity<Map<String, Object>> decreaseStock(@RequestBody InventoryUpdateRequest request) { // 1. 修改返回类型
        try {
            inventoryService.decreaseStock(request.getProductId(), request.getQuantity());

            // 2. 构建成功响应
            String port = environment.getProperty("local.server.port");
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "Stock decreased successfully.");
            responseBody.put("port",port);

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            // 保持原有的错误处理逻辑
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    // 增加库存的接口
    @PostMapping("/increase")
    public ResponseEntity<Map<String, Object>> increaseStock(@RequestBody InventoryUpdateRequest request) { // 1. 修改返回类型
        inventoryService.increaseStock(request.getProductId(), request.getQuantity());

        // 2. 构建成功响应
        String port = environment.getProperty("local.server.port");
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", "Stock increased successfully.");
        responseBody.put("port",port);

        return ResponseEntity.ok(responseBody);
    }
}
