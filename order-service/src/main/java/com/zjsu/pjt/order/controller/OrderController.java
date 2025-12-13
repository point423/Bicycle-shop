package com.zjsu.pjt.order.controller;

import com.zjsu.pjt.order.model.Order;
import com.zjsu.pjt.order.repository.OrderRepository;
import com.zjsu.pjt.order.exception.ResourceNotFoundException;
import com.zjsu.pjt.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

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
    public Order createOrder(@RequestHeader("X-User-Id") String userId,
                             @RequestHeader("X-Username") String username,
                             @Valid @RequestBody CreateOrderRequest request) {
        log.info("用户 {} (ID: {}) 发起选课请求", username, userId);
        System.out.println("====== Received request body: " + request.toString() + " ======");
        return orderService.createOrder(request.getBuyerId(), request.getProductId(), request.getQuantity());
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消订单")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID id) {
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
            @Parameter(description = "订单状态 (例如: active, CANCELLED)") @RequestParam(required = false) String status,
            @Parameter(description = "查询开始时间 (格式: yyyy-MM-dd'T'HH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "查询结束时间 (格式: yyyy-MM-dd'T'HH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        // 根据传入的参数组合调用不同的查询方法
        if (buyerId != null && status != null) {
            return orderRepository.findByBuyerIdAndStatus(buyerId, status);
        } else if (buyerId != null) {
            return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
        } else if (status != null) {
            return orderRepository.findByStatus(status);
        } else if (startDate != null && endDate != null) {
            return orderRepository.findByCreatedAtBetween(startDate, endDate);
        }
        // 如果没有提供任何查询参数，返回所有订单
        return orderRepository.findAll();
    }

    // 使用一个静态内部类来封装创建订单的请求体，使API更清晰
    @lombok.Data
    static class CreateOrderRequest {
        private UUID buyerId;
        private UUID productId;
        private Integer quantity;
    }


}
