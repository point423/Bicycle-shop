package com.zjsu.pjt.order.client;

import com.zjsu.pjt.order.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * UserClient 的降级处理实现。
 * 当 user-service 不可用或调用超时，会触发此类中的方法。
 */
@Component
@Slf4j
public class UserClientFallback implements UserClient {

    @Override
    public ResponseEntity<Map<String, Object>> getUserById(UUID userId) {
        log.warn("UserClient#getUserById fallback triggered for userId: {}", userId);
        // 直接抛出异常，让调用方（OrderService）的try-catch块捕获
        // 这样可以中断业务流程，例如创建订单
        throw new BusinessException("用户服务暂时不可用，请稍后再试", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
