package com.zjsu.pjt.product.service;

import com.zjsu.pjt.product.model.Product;
import com.zjsu.pjt.product.exception.ResourceNotFoundException;
import com.zjsu.pjt.product.repository.ProductRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; 
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
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map; 
import java.util.UUID;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    //  在Service层注入RestTemplate和环境变量 ---
    @Autowired
    private RestTemplate restTemplate;

    @Value("${inventory.service.url}") 
    private String inventoryServiceUrl;

    /**
     * 新增商品，并通知库存服务创建记录
     */
    @Transactional
    public Product createProduct(Product product) {
        // 1. 先保存商品到数据库
        Product savedProduct = productRepository.save(product);

        // 2. 在Service层调用inventory-service创建库存记录
        try {
            String createInventoryUrl = inventoryServiceUrl + "/api/inventory/create";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 请求体
            Map<String, Object> requestBody = Map.of(
                    "productId", savedProduct.getId().toString(),
                    "stock", 0 // 初始库存为0
            );

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            restTemplate.postForEntity(createInventoryUrl, requestEntity, String.class);
            System.out.println("成功通知库存服务为商品 " + savedProduct.getId() + " 创建初始库存记录。");

        } catch (Exception e) {
            System.err.println("错误：调用库存服务创建记录失败！商品ID: " + savedProduct.getId() + "。原因: " + e.getMessage());
            // 生产环境中，这里应抛出异常以回滚事务，或使用MQ保证最终一致性
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

        // 更新逻辑保持不变
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
            // 1. 先删除商品
            productRepository.deleteById(id);

            // 2.  在Service层调用inventory-service删除库存记录
            try {
                String deleteInventoryUrl = inventoryServiceUrl + "/api/inventory/" + id.toString();
                restTemplate.delete(deleteInventoryUrl);
                System.out.println("成功通知库存服务删除商品 " + id + " 的库存记录。");
            } catch (Exception e) {
                System.err.println("错误：调用库存服务删除记录失败！商品ID: " + id + "。原因: " + e.getMessage());
                // 生产环境中，这里需要有补偿机制，因为商品已删，库存成了孤儿数据
                throw new RuntimeException("删除商品成功，但通知库存服务失败！", e);
            }
            return true;
        }
        return false;
    }

    // --- 获取上架商品的核心逻辑 ---
    public List<Product> findOnShelfProducts() {
        // 1. 调用 inventory-service 获取所有上架商品的 ID 列表
        List<UUID> onShelfProductIds = getOnShelfProductIdsFromInventoryService();

        // 2. 如果没有上架的商品，直接返回空列表
        if (onShelfProductIds == null || onShelfProductIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 根据ID列表，从本地数据库查询商品详情
        return productRepository.findAllById(onShelfProductIds);
    }

    // 您还可以添加一个按分类查询上架商品的方法
    public List<Product> findOnShelfProductsByCategory(String category) {
        List<UUID> onShelfProductIds = getOnShelfProductIdsFromInventoryService();
        if (onShelfProductIds == null || onShelfProductIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 使用自定义查询（下一步会创建）
        return productRepository.findByIdInAndCategory(onShelfProductIds, category);
    }

    /**
     * 更新商品的上架/下架状态
     */
    @Transactional
    public void updateOnShelfStatus(UUID productId, boolean onShelf) {
        // 1. 验证商品是否存在
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("找不到ID为 " + productId + " 的商品");
        }

        // 2. 调用库存服务更新上架状态
        try {
            String updateStatusUrl = inventoryServiceUrl + "/api/inventory/" + productId.toString() + "/on-shelf?onShelf=" + onShelf;

            restTemplate.exchange(updateStatusUrl, HttpMethod.PUT, null, Void.class);

            System.out.println("成功通知库存服务将商品 " + productId + " 的状态更新为: " + (onShelf ? "上架" : "下架"));

        } catch (Exception e) {
            System.err.println("错误：调用库存服务更新商品状态失败！商品ID: " + productId + "。原因: " + e.getMessage());
            // 抛出运行时异常
            throw new RuntimeException("调用库存服务失败！", e);
        }
    }

    // --- 私有辅助方法，用于和 inventory-service 通信 ---
    private List<UUID> getOnShelfProductIdsFromInventoryService() {
        try {
            String url = inventoryServiceUrl + "/api/inventory/on-shelf-product-ids";

            // 【关键】因为返回的是List<String>，需要用exchange和ParameterizedTypeReference来正确反序列化
            ResponseEntity<List<String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<String>>() {}
            );

            // 将List<String>转换为List<UUID>
            return response.getBody().stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("错误：调用库存服务获取上架商品ID列表失败！原因: " + e.getMessage());
            // 在这种情况下，安全的做法是返回一个空列表，避免前台出错
            return Collections.emptyList();
        }

}}
