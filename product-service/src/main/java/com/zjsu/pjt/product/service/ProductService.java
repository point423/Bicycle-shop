package com.zjsu.pjt.product.service;

import com.zjsu.pjt.product.client.InventoryClient; // 引入Feign客户端
import com.zjsu.pjt.product.dto.InventoryCreateRequest; // 引入DTO
import com.zjsu.pjt.product.exception.BusinessException; // 引入自定义业务异常
import com.zjsu.pjt.product.model.Product;
import com.zjsu.pjt.product.exception.ResourceNotFoundException;
import com.zjsu.pjt.product.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j // 使用Lombok的Slf4j进行日志记录
public class ProductService {

    @Autowired
    private ProductRepository productRepository;



    // 注入 Feign 客户端
    @Autowired
    private InventoryClient inventoryClient;

    /**
     * 新增商品，并通知库存服务创建记录 (已使用Feign改造)
     */
    @Transactional
    public Product createProduct(Product product) {
        Product savedProduct = productRepository.save(product);

        try {
            log.info("正在调用库存服务为新商品创建库存记录, productId: {}", savedProduct.getId());
            // 使用DTO，更类型安全
            InventoryCreateRequest request = new InventoryCreateRequest(savedProduct.getId(), 0);
            ResponseEntity<Map<String, Object>> responseEntity = inventoryClient.createInventoryRecord(request);

            if (responseEntity.getBody() != null && responseEntity.getBody().containsKey("port")) {
                log.info("请求 Inventory Service 创建库存记录成功, 实例端口: {}", responseEntity.getBody().get("port"));
            }
        } catch (Exception e) {
            log.error("错误：调用库存服务创建记录失败！商品ID: {}. 原因: {}", savedProduct.getId(), e.getMessage());
            // 抛出异常以回滚事务
            throw new BusinessException("创建商品成功，但通知库存服务失败！", HttpStatus.INTERNAL_SERVER_ERROR);
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
     * 删除商品，并通知库存服务删除记录 (已使用Feign改造)
     */
    @Transactional
    public void deleteProduct(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("删除失败：找不到ID为 " + id + " 的商品");
        }

        productRepository.deleteById(id);

        try {
            log.info("正在通知库存服务删除商品 {} 的库存记录。", id);
            inventoryClient.deleteInventoryRecord(id);
            log.info("成功通知库存服务删除商品 {} 的库存记录。", id);
        } catch (Exception e) {
            log.error("错误：调用库存服务删除记录失败！商品ID: {}. 原因: {}", id, e.getMessage());
            // 抛出异常以回滚商品删除操作
            throw new BusinessException("删除商品成功，但通知库存服务失败！", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 更新商品的上架/下架状态 (已使用Feign改造)
     */
    @Transactional
    public void updateOnShelfStatus(UUID productId, boolean onShelf) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("找不到ID为 " + productId + " 的商品");
        }
        try {
            log.info("正在调用库存服务更新商品 {} 的上下架状态为: {}", productId, onShelf);
            ResponseEntity<Map<String, Object>> responseEntity = inventoryClient.updateOnShelfStatus(productId, onShelf);
            if (responseEntity.getBody() != null && responseEntity.getBody().containsKey("port")) {
                log.info("请求 Inventory Service 更新上下架状态成功, 实例详情: {}", responseEntity.getBody().get("port"));
            }
        } catch (Exception e) {
            log.error("错误：调用库存服务更新商品状态失败！商品ID: {}. 原因: {}", productId, e.getMessage());
            throw new BusinessException("调用库存服务失败！", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 查询所有上架商品 (已使用Feign改造)
     */
    public List<Product> findOnShelfProducts() {
        List<UUID> onShelfProductIds = getOnShelfProductIdsFromInventoryService();
        if (onShelfProductIds.isEmpty()) {
            return Collections.emptyList();
        }
        return productRepository.findAllById(onShelfProductIds);
    }

    /**
     * 按分类查询上架商品 (已使用Feign改造)
     */
    public List<Product> findOnShelfProductsByCategory(String category) {
        List<UUID> onShelfProductIds = getOnShelfProductIdsFromInventoryService();
        if (onShelfProductIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 使用JPA派生查询
        return productRepository.findByIdInAndCategory(onShelfProductIds, category);
    }

    // --- 私有辅助方法，用于和 inventory-service 通信 (已使用Feign改造) ---
    private List<UUID> getOnShelfProductIdsFromInventoryService() {
        try {
            log.info("正在调用库存服务获取上架商品ID列表...");
            ResponseEntity<Map<String, Object>> responseEntity = inventoryClient.getOnShelfProductIds();
            Map<String, Object> responseBody = responseEntity.getBody();

            if (responseBody == null) {
                log.warn("调用库存服务获取上架商品ID列表时返回了空的响应体！");
                return Collections.emptyList();
            }

            log.info("请求 Inventory Service 成功, 实例详情: {}", responseBody.get("port"));

            // 从 Map 的 "data" 字段中提取列表
            List<String> idStringList = (List<String>) responseBody.get("data");

            if (idStringList == null) {
                log.warn("响应体中缺少 'data' 字段或其值为null！");
                return Collections.emptyList();
            }

            return idStringList.stream().map(UUID::fromString).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("错误：调用库存服务获取上架商品ID列表失败！原因: {}", e.getMessage());
            // 在查询场景下，服务失败不应中断整个应用，返回空列表是合理的降级策略
            return Collections.emptyList();
        }
    }
}
