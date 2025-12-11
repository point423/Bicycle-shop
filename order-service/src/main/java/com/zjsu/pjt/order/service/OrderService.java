package com.zjsu.pjt.order.service;

import com.zjsu.pjt.order.model.Order;
import com.zjsu.pjt.order.repository.OrderRepository;
import com.zjsu.pjt.order.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 创建订单
     * @param buyerId 购买者ID
     * @param productId 商品ID
     * @param quantity 购买数量
     * @return 创建成功的订单对象
     */
    @Transactional
    public Order createOrder(UUID buyerId, UUID productId, int quantity) {
        // 1. 调用 User-Service 验证用户是否存在
        if (!isUserExists(buyerId)) {
            throw new ResourceNotFoundException("创建订单失败：购买者用户不存在，ID: " + buyerId);
        }

        // 2. 直接调用 Inventory-Service 扣减库存
        // inventory-service 内部会处理库存不足的情况
        deductStock(productId, quantity);

        // 3. 如果库存扣减成功，创建并保存订单
        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setStatus("active"); // 初始状态为 'active'

        return orderRepository.save(order);
    }

    /**
     * 取消订单
     * @param orderId 要取消的订单ID
     */
    @Transactional
    public void cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在，ID: " + orderId));

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("订单已取消，请勿重复操作");
        }

        // 1. 调用 Inventory-Service 回滚（增加）库存
        increaseStock(order.getProductId(), order.getQuantity());

        // 2. 更新订单状态为 "CANCELLED"
        order.setStatus("CANCELLED");
        orderRepository.save(order);
    }

    // --- 私有辅助方法，用于和各个微服务通信 ---

    /**
     * 调用 user-service 检查用户是否存在
     */
    private boolean isUserExists(UUID userId) {
        String url = "http://user-service/api/users/" + userId.toString();
        try {
            // 使用 exchange 和 ParameterizedTypeReference 来正确处理 Map 响应
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            // 检查响应体并打印端口
            if (responseEntity.getBody() != null && responseEntity.getBody().containsKey("port")) {
                System.out.println("请求 User Service 验证用户成功, 实例详情: " + responseEntity.getBody().get("port"));
            }

            return responseEntity.getStatusCode().is2xxSuccessful();

        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            System.err.println("调用用户服务验证用户时发生错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 调用 inventory-service 扣减库存
     */
    private void deductStock(UUID productId, int quantity) {
        String url = "http://inventory-service/api/inventory/decrease";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> requestBody = Map.of(
                "productId", productId.toString(),
                "quantity", quantity
        );
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // 使用 exchange 和 ParameterizedTypeReference
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (responseEntity.getBody() != null && responseEntity.getBody().containsKey("port")) {
                System.out.println("请求 Inventory Service 扣减库存成功，实例详情: " + responseEntity.getBody().get("port"));
            }
        } catch (HttpClientErrorException e) {
            throw new IllegalStateException("创建订单失败：库存不足或商品不存在 - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("调用库存服务失败: " + e.getMessage());
        }
    }

    /**
     * 调用 inventory-service 增加库存
     */
    private void increaseStock(UUID productId, int quantity) {
        String url = "http://inventory-service/api/inventory/increase";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> requestBody = Map.of(
                "productId", productId.toString(),
                "quantity", quantity
        );
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // 使用 exchange 和 ParameterizedTypeReference
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (responseEntity.getBody() != null && responseEntity.getBody().containsKey("port")) {
                System.out.println("请求 Inventory Service 增加库存成功，实例详情: " + responseEntity.getBody().get("port"));
            }
        } catch (Exception e) {
            throw new RuntimeException("取消订单警告：回滚库存失败 - " + e.getMessage());
        }
    }
}
