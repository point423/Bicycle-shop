package com.zjsu.pjt.product.service;

import com.zjsu.pjt.product.model.Product;
import com.zjsu.pjt.product.exception.ResourceNotFoundException;
import com.zjsu.pjt.product.repository.ProductRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;



    /**
     * 新增商品
     * @param product 要创建的商品对象
     * @return 已保存的商品对象
     */
    @Transactional
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }


    /**
     * 修改商品
     * @param id 要修改的商品ID
     * @param productDetails 包含新信息的商品对象
     * @return 更新后的商品对象
     */
    @Transactional
    public Product updateProduct(UUID id, Product productDetails) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到ID为 " + id + " 的商品"));

        // 更新现有商品的每个字段
        existingProduct.setBrand(productDetails.getBrand());
        existingProduct.setCategory(productDetails.getCategory());
        existingProduct.setModel(productDetails.getModel());
        existingProduct.setGearSystem(productDetails.getGearSystem());
        existingProduct.setFrameSize(productDetails.getFrameSize());
        existingProduct.setColor(productDetails.getColor());
        existingProduct.setPrice(productDetails.getPrice());
        existingProduct.setStock(productDetails.getStock());
        existingProduct.setOnShelf(productDetails.isOnShelf());

        // 保存并返回更新后的商品
        return productRepository.save(existingProduct);
    }

    /**
     * 删除商品
     * @param id 要删除的商品ID
     * @return 如果找到并删除，返回true；否则返回false
     */
    @Transactional
    public boolean deleteProduct(UUID id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    
    /**
     * 扣减库存 (核心)
     * @param productId 商品ID
     * @param quantity 扣减数量
     */
    @Transactional
    public void deductStock(UUID productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + productId));

        if (product.getStock() < quantity) {
            throw new IllegalStateException("库存不足");
        }
        product.setStock(product.getStock() - quantity);

        try {
            productRepository.save(product);
        } catch (OptimisticLockException ex) {
            // TODO: // 当并发更新时，乐观锁会抛出此异常
            throw new IllegalStateException("库存更新失败，请重试", ex);
        }
    }

    /**
     * 增加库存 (用于取消订单)
     * @param productId 商品ID
     * @param quantity 增加数量
     */
    @Transactional
    public void increaseStock(UUID productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在: " + productId));

        product.setStock(product.getStock() + quantity);
        productRepository.save(product);
    }
}
