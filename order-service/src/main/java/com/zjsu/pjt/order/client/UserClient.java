package com.zjsu.pjt.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.UUID;

/**
 * Feign客户端，用于与 user-service 进行通信。
 * name = "user-service" 必须与用户服务在注册中心的名称一致。
 * fallback 指定了当服务不可用时的降级处理类。
 */
@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {

    /**
     * 调用 user-service, 根据用户ID获取用户信息。
     * OrderService 用它来验证用户是否存在。
     * @param userId 用户的UUID
     * @return 包含用户信息的响应实体。期望响应体是一个Map，其中包含 "port" 字段。
     */
    // 这里的路径必须和 user-service 的 Controller 中的路径完全匹配
    @GetMapping("/api/users/{userId}")
    ResponseEntity<Map<String, Object>> getUserById(@PathVariable("userId") UUID userId);
}
