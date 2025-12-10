package com.zjsu.pjt.order.service;

import com.zjsu.pjt.order.model.Order;
import com.zjsu.pjt.order.repository.OrderRepository;
import com.zjsu.pjt.order.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestTemplate restTemplate;



    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    @Value("${user.service.url}")
    private String userServiceUrl;
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

    // --- 私有辅助方法，用于和 Inventory-Service 通信 ---


    /**
     * 新增：调用 user-service 检查用户是否存在
     * @param userId 要检查的用户ID
     * @return 如果用户存在，返回true；否则返回false
     */
    private boolean isUserExists(UUID userId) {
        // 假设 user-service 提供了一个 GET /api/users/{id} 的接口
       URI uri = UriComponentsBuilder
                .fromHttpUrl(userServiceUrl)
                .path("/api/users/{id}")
                .buildAndExpand(userId) // 使用 buildAndExpand 替换路径变量
                .toUri();
        try {
            // 使用 exchange 方法可以更好地处理不同状态码
            // 我们只关心请求是否成功（2xx），不关心返回体内容
            ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.GET, null, Void.class);
            // 如果状态码是 2xx，说明用户存在
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.NotFound e) {
            // 如果收到 404 Not Found，明确表示用户不存在
            return false;
        } catch (Exception e) {
            // 对于其他网络或服务器错误，我们也可以认为验证失败
            // 生产环境中应记录详细日志
            System.err.println("调用用户服务验证用户时发生错误: " + e.getMessage());
            return false;
        }
    }



    /**
     * 调用 inventory-service 扣减库存
     */
    private void deductStock(UUID productId, int quantity) {
        // 构造指向 inventory-service 的URL
        String url = inventoryServiceUrl + "/api/inventory/decrease";

        // 准备请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 准备请求体 (Map 或 DTO)
        Map<String,Object> requestBody = Map.of(
                "productId", productId.toString(),
                "quantity", quantity
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // 发起POST请求
            restTemplate.postForEntity(url, requestEntity, Void.class);
        } catch (HttpClientErrorException e) {
            // 如果 inventory-service 返回4xx错误（如400 Bad Request代表库存不足），则抛出异常
            throw new IllegalStateException("创建订单失败：库存不足或商品不存在 - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            // 处理其他网络或服务器错误
            throw new RuntimeException("调用库存服务失败: " + e.getMessage());
        }
    }


    /**
     * 调用 inventory-service 增加库存
     */
    private void increaseStock(UUID productId, int quantity) {
        // 构造指向 inventory-service 的URL
        String url = inventoryServiceUrl + "/api/inventory/increase";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
                "productId", productId.toString(),
                "quantity", quantity
        );

       HttpEntity<java.util.Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            // 发起POST请求
            restTemplate.postForEntity(url, requestEntity, Void.class);
        } catch (Exception e) {
            // 如果回滚库存失败，也应抛出异常并记录日志。
            // 在实际生产中，这里可能需要加入重试或记录到失败任务表，由定时任务补偿。
            throw new RuntimeException("取消订单警告：回滚库存失败 - " + e.getMessage());
        }
    }
}
