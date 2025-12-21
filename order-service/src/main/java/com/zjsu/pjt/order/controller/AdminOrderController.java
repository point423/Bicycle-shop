package com.zjsu.pjt.order.controller;

import com.zjsu.pjt.order.client.ProductClient;
import com.zjsu.pjt.order.dto.ProductClientDTO;
import com.zjsu.pjt.order.dto.AdminOrderDTO;
import com.zjsu.pjt.order.model.Order;
import com.zjsu.pjt.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderRepository orderRepository;

    // 只需要 ProductClient，删除了 UserClient
    private final ProductClient productClient;

    @GetMapping("/all")
    public ResponseEntity<List<AdminOrderDTO>> getAllOrders() {
        List<Order> orders = orderRepository.findAll();

        List<AdminOrderDTO> dtos = orders.stream().map(order -> {
            AdminOrderDTO dto = new AdminOrderDTO();

            // 1. 复制订单基本信息 (包含 buyerId)
            dto.setId(order.getId());
            dto.setProductId(order.getProductId());
            dto.setBuyerId(order.getBuyerId()); // 这里已经设置了 BuyerID
            dto.setQuantity(order.getQuantity());
            dto.setStatus(order.getStatus());
            dto.setCreatedAt(order.getCreatedAt());

            // 2. 远程调用商品服务获取信息
            try {
                // 注意：根据之前的修改，Client 返回的是 ResponseEntity，需要 .getBody()
                ResponseEntity<ProductClientDTO> response = productClient.getProductById(order.getProductId());

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    ProductClientDTO product = response.getBody();

                    dto.setProductBrand(product.getBrand());
                    dto.setProductModel(product.getModel());
                    dto.setProductCategory(product.getCategory());
                    dto.setPrice(product.getPrice());
                    dto.setProductImage(product.getImageUrl());
                }
            } catch (Exception e) {
                // 如果商品服务调用失败，设置默认值
                dto.setProductModel("未知商品 (获取失败)");
            }


            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID id) {
        if (!orderRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        orderRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}