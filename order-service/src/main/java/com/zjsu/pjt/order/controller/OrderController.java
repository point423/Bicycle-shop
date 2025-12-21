package com.zjsu.pjt.order.controller;

import com.zjsu.pjt.order.model.Order;
import com.zjsu.pjt.order.repository.OrderRepository;
import com.zjsu.pjt.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Data
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @PostMapping
    @Operation(summary = "创建新订单")
    @ResponseStatus(HttpStatus.CREATED)
    public Order createOrder(@RequestHeader(value = "X-User-Id", required = false) String userId,
                             @RequestHeader(value = "X-Username", required = false) String username,
                             @Valid @RequestBody CreateOrderRequest request) {
        log.info("用户 {} (ID: {}) 发起选课请求", username, userId);
        return orderService.createOrder(request.getBuyerId(), request.getProductId(), request.getQuantity());
    }

    // --- 新增：适配前端的 GET /api/orders/user/{userId} ---
    @GetMapping("/user/{userId}")
    @Operation(summary = "获取指定用户的所有订单")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable UUID userId) {
        // 使用 Repository 中已有的方法
        List<Order> orders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(orders);
    }

    // --- 新增/修改：适配前端的 DELETE /api/orders/{id} ---
    // 前端调用 DELETE 方法来取消订单
    @DeleteMapping("/{id}")
    @Operation(summary = "删除订单 (取消订单并回滚库存)")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID id) {
        // 调用 Service 的取消逻辑 (里面包含了库存回滚)
        orderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }

    // 保留原有的 PUT cancel 接口 (可选，为了兼容性)
    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消订单 (PUT方式)")
    public ResponseEntity<Void> cancelOrderPut(@PathVariable UUID id) {
        orderService.cancelOrder(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取订单详情")
    public ResponseEntity<Order> getOrderById(@PathVariable UUID id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @Operation(summary = "按多种条件查询订单")
    public List<Order> searchOrders(
            @Parameter(description = "购买者ID") @RequestParam(required = false) UUID buyerId,
            @Parameter(description = "订单状态") @RequestParam(required = false) String status,
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        if (buyerId != null && status != null) {
            return orderRepository.findByBuyerIdAndStatus(buyerId, status);
        } else if (buyerId != null) {
            return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
        } else if (status != null) {
            return orderRepository.findByStatus(status);
        } else if (startDate != null && endDate != null) {
            return orderRepository.findByCreatedAtBetween(startDate, endDate);
        }
        return orderRepository.findAll();
    }

    @lombok.Data
    static class CreateOrderRequest {
        private UUID buyerId;
        private UUID productId;
        private Integer quantity;
    }
}