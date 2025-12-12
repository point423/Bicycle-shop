package com.zjsu.pjt.order.client;

import com.zjsu.pjt.order.dto.InventoryUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Feign 客户端，用于与 inventory-service 进行通信。
 * name = "inventory-service" 必须与库存服务在Nacos/Eureka中注册的名称一致。
 * fallback 指定了当 inventory-service 不可用时的降级处理类。
 */
@FeignClient(name = "inventory-service", fallback = InventoryClientFallback.class)
public interface InventoryClient {

    /**
     * 调用 inventory-service 扣减指定商品的库存。
     * 路径和参数必须与 InventoryController 中的定义完全匹配。
     *
     * @param request 包含商品ID和扣减数量的请求体
     * @return 返回一个包含操作结果和端口信息的Map
     */
    @PostMapping("/api/inventory/decrease")
    ResponseEntity<Map<String, Object>> decreaseStock(@RequestBody InventoryUpdateRequest request);


    /**
     * 调用 inventory-service 增加指定商品的库存 (用于取消订单时的库存回滚)。
     * @param request 包含商品ID和增加数量的请求体
     * @return 包含操作结果的响应实体
     */
    @PostMapping("/api/inventory/increase")
    ResponseEntity<java.util.Map<String, Object>> increaseStock(@RequestBody InventoryUpdateRequest request);

}
