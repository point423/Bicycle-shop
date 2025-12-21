package com.zjsu.pjt.product.client;

import com.zjsu.pjt.product.dto.InventoryCreateRequest;
import com.zjsu.pjt.product.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.List; // 导入 List
import com.zjsu.pjt.product.dto.InventoryUpdateRequest; // 确保你创建了这个DTO


@Component
@Slf4j
public class InventoryClientFallback implements InventoryClient {

    private static final String UNAVAILABLE_MSG = "库存服务暂时不可用，请稍后再试";

    @Override
    public ResponseEntity<Map<String, Object>> createInventoryRecord(InventoryCreateRequest request) {
        log.warn("InventoryClient#createInventoryRecord fallback for productId: {}", request.getProductId());
        throw new BusinessException(UNAVAILABLE_MSG, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public void deleteInventoryRecord(UUID productId) {
        log.warn("InventoryClient#deleteInventoryRecord fallback for productId: {}", productId);
        throw new BusinessException(UNAVAILABLE_MSG, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public ResponseEntity<Map<String, Object>> updateOnShelfStatus(UUID productId, boolean onShelf) {
        log.warn("InventoryClient#updateOnShelfStatus fallback for productId: {}", productId);
        throw new BusinessException(UNAVAILABLE_MSG, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public ResponseEntity<Map<String, Object>> getOnShelfProductIds() {
        log.warn("InventoryClient#getOnShelfProductIds fallback triggered. 返回空列表作为降级响应。");
        // 对于查询操作，返回一个空的成功响应是比抛出异常更好的降级策略
        Map<String, Object> fallbackResponse = Map.of(
                "data", Collections.emptyList(),
                "port", "fallback",
                "message", "Service unavailable, returning empty list."
        );
        return ResponseEntity.ok(fallbackResponse);
    }

    @Override
    public Map<UUID, Integer> getStocksByProductIds(List<UUID> productIds) {
        log.warn("InventoryClient#getStocksByProductIds fallback triggered for {} product IDs. 返回空的库存信息作为降级响应。", productIds.size());
        // 对于批量查询库存，返回一个空的Map是合理的降级策略
        return Collections.emptyMap();
    }


    @Override
    public ResponseEntity<Map<String, Object>> decreaseStock(InventoryUpdateRequest request) {
        log.warn("InventoryClient#decreaseStock fallback for productId: {}", request.getProductId());
        throw new BusinessException(UNAVAILABLE_MSG, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public ResponseEntity<Map<String, Object>> increaseStock(InventoryUpdateRequest request) {
        log.warn("InventoryClient#increaseStock fallback for productId: {}", request.getProductId());
        throw new BusinessException(UNAVAILABLE_MSG, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
