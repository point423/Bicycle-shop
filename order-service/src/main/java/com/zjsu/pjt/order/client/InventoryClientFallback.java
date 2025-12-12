package com.zjsu.pjt.order.client;

import com.zjsu.pjt.order.exception.BusinessException;
import com.zjsu.pjt.order.dto.InventoryUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class InventoryClientFallback implements InventoryClient {

    private static final String SERVICE_UNAVAILABLE_MESSAGE = "库存服务暂时不可用，请稍后再试";

    @Override
    public ResponseEntity<Map<String, Object>> decreaseStock(InventoryUpdateRequest request) {
        log.warn("InventoryClient#decreaseStock fallback triggered for productId: {}", request.getProductId());
        // 对于写操作（如扣减库存），直接抛出异常是合适的，这样可以触发事务回滚，告知用户下单失败
        throw new BusinessException(SERVICE_UNAVAILABLE_MESSAGE, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public ResponseEntity<Map<String, Object>> increaseStock(InventoryUpdateRequest request) {
        log.warn("InventoryClient#increaseStock fallback triggered for productId: {}", request.getProductId());
        // 增加库存（回滚）失败是一个严重问题，同样应抛出异常
        // OrderService 会捕获此异常并阻止订单状态被错误地更新为 "CANCELLED"
        throw new BusinessException(SERVICE_UNAVAILABLE_MESSAGE, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
