package com.zjsu.pjt.product.service;

import com.zjsu.pjt.product.model.Product;
import com.zjsu.pjt.product.exception.ResourceNotFoundException;
import com.zjsu.pjt.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 新增商品，并通知库存服务创建记录
     */
    @Transactional
    public Product createProduct(Product product) {
        Product savedProduct = productRepository.save(product);

        try {
            String url = "http://inventory-service/api/inventory/create";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> requestBody = Map.of("productId", savedProduct.getId().toString(), "stock", 0);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, requestEntity, Map.class);
            if (responseEntity.getBody() != null && responseEntity.getBody().containsKey("port")) {
                System.out.println("请求 Inventory Service 创建库存记录成功, 实例端口: " + responseEntity.getBody().get("port"));
            }
        } catch (Exception e) {
            System.err.println("错误：调用库存服务创建记录失败！商品ID: " + savedProduct.getId() + "。原因: " + e.getMessage());
            throw new RuntimeException("创建商品成功，但通知库存服务失败！", e);
        }

        return savedProduct;
    }

    /**
     * 修改商品信息
     */
    @Transactional
    public Product updateProduct(UUID id, Product productDetails) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID为 " + id + " 的商品"));

        existingProduct.setBrand(productDetails.getBrand());
        existingProduct.setCategory(productDetails.getCategory());
        existingProduct.setModel(productDetails.getModel());
        existingProduct.setGearSystem(productDetails.getGearSystem());
        existingProduct.setFrameSize(productDetails.getFrameSize());
        existingProduct.setColor(productDetails.getColor());
        existingProduct.setPrice(productDetails.getPrice());

        return productRepository.save(existingProduct);
    }

    /**
     * 删除商品，并通知库存服务删除记录
     */
    @Transactional
    public boolean deleteProduct(UUID id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            try {
                String url = "http://inventory-service/api/inventory/" + id.toString();
                restTemplate.delete(url);
                System.out.println("成功通知库存服务删除商品 " + id + " 的库存记录。");
            } catch (Exception e) {
                System.err.println("错误：调用库存服务删除记录失败！商品ID: " + id + "。原因: " + e.getMessage());
                throw new RuntimeException("删除商品成功，但通知库存服务失败！", e);
            }
            return true;
        }
        return false;
    }

    public List<Product> findOnShelfProducts() {
        List<UUID> onShelfProductIds = getOnShelfProductIdsFromInventoryService();
        if (onShelfProductIds == null || onShelfProductIds.isEmpty()) {
            return Collections.emptyList();
        }
        return productRepository.findAllById(onShelfProductIds);
    }

    // 一个按分类查询上架商品的方法
    public List<Product> findOnShelfProductsByCategory(String category) {
        List<UUID> onShelfProductIds = getOnShelfProductIdsFromInventoryService();
        if (onShelfProductIds == null || onShelfProductIds.isEmpty()) {
            return Collections.emptyList();
        }
        return productRepository.findByIdInAndCategory(onShelfProductIds, category);
    }

    /**
     * 更新商品的上架/下架状态
     */
    @Transactional
    public void updateOnShelfStatus(UUID productId, boolean onShelf) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("找不到ID为 " + productId + " 的商品");
        }
        try {
            String url = "http://inventory-service/api/inventory/" + productId.toString() + "/on-shelf?onShelf=" + onShelf;

            // 使用 exchange 和 ParameterizedTypeReference 来期望一个 Map
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    null, // PUT 请求通常可以没有请求体，特别是当所有信息都在URL中时
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            // 检查响应体并打印端口信息
            if (responseEntity.getBody() != null && responseEntity.getBody().containsKey("port")) {
                System.out.println("请求 Inventory Service 更新上下架状态成功, 实例详情: " + responseEntity.getBody().get("port"));
            }

        } catch (Exception e) {
            System.err.println("错误：调用库存服务更新商品状态失败！商品ID: " + productId + "。原因: " + e.getMessage());
            // 抛出运行时异常，确保事务能回滚（如果适用）
            throw new RuntimeException("调用库存服务失败！", e);
        }
    }

    // --- 私有辅助方法，用于和 inventory-service 通信 ---
    private List<UUID> getOnShelfProductIdsFromInventoryService() {
        String url = "http://inventory-service/api/inventory/on-shelf-product-ids";
        try {
            // 【关键修改】期望一个 Map，其键是 String，值是 Object
            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {} // <-- 告诉 RestTemplate 期望一个 Map
            );

            Map<String, Object> responseBody = responseEntity.getBody();

            if (responseBody == null) {
                System.err.println("错误：调用库存服务获取上架商品ID列表时返回了空的响应体！");
                return Collections.emptyList();
            }

            // 【新增】打印端口号信息
            System.out.println("请求 Inventory Service 成功, 实例详情: " + responseBody.get("port"));

            // 【修改】从 Map 的 "data" 字段中提取列表
            // 注意：RestTemplate 反序列化 JSON 数组时，默认会将其中的元素（UUID字符串）作为 String
            List<String> idStringList = (List<String>) responseBody.get("data");

            if (idStringList == null) {
                System.err.println("错误：响应体中缺少 'data' 字段或其值为null！");
                return Collections.emptyList();
            }

            // 【修改】将字符串列表转换为 UUID 列表
            return idStringList.stream().map(UUID::fromString).collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("错误：调用库存服务获取上架商品ID列表失败！原因: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
