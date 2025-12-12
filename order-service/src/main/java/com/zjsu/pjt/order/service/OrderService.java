package com.zjsu.pjt.order.service;

import com.zjsu.pjt.order.client.InventoryClient;
import com.zjsu.pjt.order.client.UserClient;
import com.zjsu.pjt.order.dto.InventoryUpdateRequest;
import com.zjsu.pjt.order.exception.BusinessException;
import com.zjsu.pjt.order.exception.ResourceNotFoundException;
import com.zjsu.pjt.order.model.Order;
import com.zjsu.pjt.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

   

    // 注入 Feign 客户端
    @Autowired
    private UserClient userClient;

    @Autowired
    private InventoryClient inventoryClient;

    /**
     * 创建订单 (已使用 OpenFeign 改造)
     * @param buyerId 购买者ID
     * @param productId 商品ID
     * @param quantity 购买数量
     * @return 创建成功的订单对象
     */
    @Transactional
    public Order createOrder(UUID buyerId, UUID productId, int quantity) {
        // 1. 调用 User-Service 验证用户是否存在 (通过 UserClient)
        try {
            log.info("正在验证用户是否存在, buyerId: {}", buyerId);
            ResponseEntity<Map<String, Object>> userResponse = userClient.getUserById(buyerId);
            if (!userResponse.getStatusCode().is2xxSuccessful() || userResponse.getBody() == null) {
                throw new ResourceNotFoundException("创建订单失败：购买者用户不存在或用户服务响应异常，ID: " + buyerId);
            }
            log.info("用户验证成功, 响应端口: {}", userResponse.getBody().get("port"));
        } catch (Exception e) {
            // 捕获 Feign 调用可能产生的异常 (包括熔断)
            throw new BusinessException("用户服务调用失败: " + e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        }

        // 2. 调用 Inventory-Service 扣减库存 (通过 InventoryClient)
        try {
            log.info("正在调用库存服务扣减库存, productId: {}, quantity: {}", productId, quantity);
            InventoryUpdateRequest request = new InventoryUpdateRequest(productId, quantity);
            ResponseEntity<Map<String, Object>> inventoryResponse = inventoryClient.decreaseStock(request);

            if (inventoryResponse.getBody() != null && inventoryResponse.getBody().containsKey("port")) {
                log.info("库存扣减成功, 响应端口: {}", inventoryResponse.getBody().get("port"));
            }
        } catch (Exception e) {
            // 这个异常可能是 Feign 熔断异常，也可能是库存服务主动抛出的业务异常（如库存不足）
            // Feign 会将服务端的异常信息包装后抛出
            log.error("库存服务调用失败或业务逻辑不通过: {}", e.getMessage());
            throw new BusinessException("创建订单失败: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // 3. 如果库存扣减成功，创建并保存订单
        log.info("所有检查通过，正在创建订单...");
        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setStatus("ACTIVE"); // 统一使用大写状态

        return orderRepository.save(order);
    }

    /**
     * 取消订单 (已使用 OpenFeign 改造)
     * @param orderId 要取消的订单ID
     */
    @Transactional
    public void cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在，ID: " + orderId));

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("订单已取消，请勿重复操作");
        }

        // 1. 调用 Inventory-Service 回滚（增加）库存 (通过 InventoryClient)
        try {
            log.info("正在调用库存服务增加库存 (回滚), productId: {}, quantity: {}", order.getProductId(), order.getQuantity());
            // 注意: `increaseStock` 方法也需要在 InventoryClient 中定义
            InventoryUpdateRequest request = new InventoryUpdateRequest(order.getProductId(), order.getQuantity());
            ResponseEntity<java.util.Map<String, Object>> inventoryResponse = inventoryClient.increaseStock(request);

            if (inventoryResponse.getBody() != null && inventoryResponse.getBody().containsKey("port")) {
                log.info("库存增加(回滚)成功, 响应端口: {}", inventoryResponse.getBody().get("port"));
            }
        } catch (Exception e) {
            // 如果回滚库存失败，这是一个严重问题，需要记录并可能需要人工介入
            log.error("取消订单时，回滚库存失败! OrderId: {}, ProductId: {}. 错误: {}", orderId, order.getProductId(), e.getMessage());
            // 抛出异常，让事务回滚，订单状态不会被更新为 CANCELLED，以便后续重试或处理
            throw new BusinessException("取消订单失败：无法回滚库存 - " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 2. 更新订单状态为 "CANCELLED"
        order.setStatus("CANCELLED");
        orderRepository.save(order);
        log.info("订单 {} 已成功取消", orderId);
    }
    
}
