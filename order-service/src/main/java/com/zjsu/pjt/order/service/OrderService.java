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

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestTemplate restTemplate;

    // 从 application.properties 中读取 product-service 的地址
    @Value("${product.service.url}")
    private String productServiceUrl;

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
        // 1.调用 User-Service 验证用户是否存在
        if (!isUserExists(buyerId)) {
            throw new ResourceNotFoundException("创建订单失败：购买者用户不存在，ID: " + buyerId);
        }


        // 2.获取商品信息并校验库存
        Map<String, Object> productInfo = getProductInfo(productId);
        int stock = (Integer) productInfo.get("stock");
        if (stock < quantity) {
            // 这里可以抛出更具体的业务异常
            throw new IllegalStateException("库存不足，当前库存: " + stock + "，需要: " + quantity);
        }
        // 3. 调用 Product-Service 扣减库存
        deductStock(productId, quantity);

        // 4. 如果库存扣减成功，创建并保存订单
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

        // 检查订单是否已经是取消状态
        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalStateException("订单已取消，请勿重复操作");
        }

        // 1. 调用 Product-Service 回滚（增加）库存
        increaseStock(order.getProductId(), order.getQuantity());

        // 2. 更新订单状态为 "CANCELLED"
        order.setStatus("CANCELLED");
        orderRepository.save(order);
    }

    // --- 私有辅助方法，用于和 Product-Service 通信 ---


    /**
     * 新增：调用 user-service 检查用户是否存在
     * @param userId 要检查的用户ID
     * @return 如果用户存在，返回true；否则返回false
     */
    private boolean isUserExists(UUID userId) {
        // 假设 user-service 提供了一个 GET /api/users/{id} 的接口
        java.net.URI uri = UriComponentsBuilder
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
     * 【新增】调用 product-service 获取商品完整信息
     */
    private Map<String, Object> getProductInfo(UUID productId) {
        URI uri = UriComponentsBuilder.fromHttpUrl(productServiceUrl).path("/api/products/{id}").buildAndExpand(productId).toUri();
        try {
            // 假设product-service的GET /api/products/{id}返回一个包含stock和price的JSON
            ResponseEntity<Map> response = restTemplate.getForEntity(uri, Map.class);
            if (response.getBody() == null) {
                throw new ResourceNotFoundException("从商品服务获取信息为空，ID: " + productId);
            }
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("商品不存在，ID: " + productId);
        }
    }



    /**
     * 调用 product-service 扣减库存的内部接口
     */
    private void deductStock(UUID productId, int quantity) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(productServiceUrl)
                .path("/api/products/internal/deduct-stock")
                .queryParam("productId", productId)
                .queryParam("quantity", quantity)
                .build().toUri();
        try {
            // 发起POST请求
            restTemplate.postForEntity(uri, null, Void.class);
        } catch (HttpClientErrorException e) {
            // 如果product-service返回错误（如库存不足），则抛出异常，事务将回滚
            throw new RuntimeException("创建订单失败：扣减库存失败 - " + e.getResponseBodyAsString());
        }
    }

    /**
     * 调用 product-service 增加库存的内部接口
     */
    private void increaseStock(UUID productId, int quantity) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(productServiceUrl)
                .path("/api/products/internal/increase-stock")
                .queryParam("productId", productId)
                .queryParam("quantity", quantity)
                .build().toUri();
        try {
            // 发起POST请求
            restTemplate.postForEntity(uri, null, Void.class);
        } catch (HttpClientErrorException e) {
            // 如果回滚库存失败，也应抛出异常，让调用方知道。
            // 在实际生产中，这里可能需要加入重试或记录日志的机制。
            throw new RuntimeException("取消订单警告：回滚库存失败 - " + e.getResponseBodyAsString());
        }
    }
}
