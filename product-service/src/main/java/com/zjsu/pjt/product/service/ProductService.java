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
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.zjsu.pjt.product.dto.ProductDetailDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
@Service
@Slf4j // 使用Lombok的Slf4j进行日志记录
public class ProductService {

    @Autowired
    private ProductRepository productRepository;



    // 注入 Feign 客户端
    @Autowired
    private InventoryClient inventoryClient;


    public List<ProductDetailDTO> findAllProductsWithStock() {
        List<Product> allProducts = productRepository.findAll();
        if (allProducts.isEmpty()) {
            return Collections.emptyList();
        }
        List<UUID> allProductIds = allProducts.stream().map(Product::getId).collect(Collectors.toList());
        Map<UUID, Integer> stockMap = inventoryClient.getStocksByProductIds(allProductIds);

        return allProducts.stream()
                .map(product -> new ProductDetailDTO(product, stockMap.getOrDefault(product.getId(), 0)))
                .collect(Collectors.toList());
    }

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


    // 在 ProductService 中添加以下方法

    /**
     * 按分类查询上架商品（分页 + 库存填充）
     * 逻辑：先获取所有上架ID，再在这些ID中按分类筛选并分页
     */
    public Page<ProductDetailDTO> findProductsByCategoryWithStock(String category, Pageable pageable) {

        // 1. 获取所有上架商品的ID列表 (从 Inventory Service)
        List<UUID> onShelfProductIds = getOnShelfProductIdsFromInventoryService();

        if (onShelfProductIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // 2. 从数据库查询：ID 在上架列表中，且分类匹配 (需要 Repository 支持)
        // 你的 Repository 可能需要加一个方法：findByIdInAndCategory(List<UUID> ids, String category, Pageable pageable)
        // 如果没有这个方法，请先去 Repository 加上
        Page<Product> productPage = productRepository.findByIdInAndCategory(onShelfProductIds, category, pageable);

        if (productPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 3. 提取当前页的商品ID
        List<UUID> productIdsOnPage = productPage.getContent().stream()
                .map(Product::getId)
                .collect(Collectors.toList());

        // 4. 批量查询库存
        Map<UUID, Integer> stockMap;
        try {
            log.info("正在批量调用库存服务获取 {} 个商品的库存 (分类: {})...", productIdsOnPage.size(), category);
            // 假设 inventoryClient.getStocksByProductIds 返回的是 Map<UUID, Integer>
            // 注意：如果你的 Client 返回的是 ResponseEntity<Map...>，请在这里加上 .getBody()
            // 根据你上面的代码 findAllProductsWithStock 来看，Client 直接返回了 Map，所以这里保持一致
            stockMap = inventoryClient.getStocksByProductIds(productIdsOnPage);
        } catch (Exception e) {
            log.error("获取库存失败", e);
            stockMap = new HashMap<>(); // 降级处理
        }

        // 5. 组装 DTO
        Map<UUID, Integer> finalStockMap = stockMap;
        List<ProductDetailDTO> dtos = productPage.getContent().stream()
                .map(product -> new ProductDetailDTO(product, finalStockMap.getOrDefault(product.getId(), 0)))
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, productPage.getTotalElements());
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
    @Transactional(readOnly = true)
    public Page<ProductDetailDTO> findOnShelfProductsWithStock(Pageable pageable) {
        // 1. 获取所有上架商品的ID列表
        List<UUID> onShelfProductIds = getOnShelfProductIdsFromInventoryService();
        if (onShelfProductIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // 2. 根据ID列表进行分页查询商品
        Page<Product> productPage = productRepository.findByIdIn(onShelfProductIds, pageable);
        List<Product> productsOnPage = productPage.getContent();

        if (productsOnPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // 3. 提取当前页的商品ID，并批量获取库存
        List<UUID> productIdsOnPage = productsOnPage.stream().map(Product::getId).collect(Collectors.toList());
        Map<UUID, Integer> stockMap;
        try {
            log.info("正在批量调用库存服务获取 {} 个商品的库存...", productIdsOnPage.size());
            stockMap = inventoryClient.getStocksByProductIds(productIdsOnPage);
            log.info("批量获取库存成功。");
        } catch (Exception e) {
            log.error("错误：批量调用库存服务失败！原因: {}", e.getMessage());
            // 降级策略：如果库存服务失败，返回空Map，前端将显示库存为0
            stockMap = Collections.emptyMap();
        }

        // 4. 组装成DTO列表
        final Map<UUID, Integer> finalStockMap = stockMap; // effectively final for lambda
        List<ProductDetailDTO> dtos = productsOnPage.stream()
                .map(product -> new ProductDetailDTO(product, finalStockMap.getOrDefault(product.getId(), 0)))
                .collect(Collectors.toList());

        // 5. 使用 PageImpl 创建并返回最终的分页结果
        return new PageImpl<>(dtos, pageable, productPage.getTotalElements());
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
