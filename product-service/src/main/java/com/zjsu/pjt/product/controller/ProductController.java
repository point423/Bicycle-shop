package com.zjsu.pjt.product.controller;

import com.zjsu.pjt.product.model.Product;
import com.zjsu.pjt.product.repository.ProductRepository;
import com.zjsu.pjt.product.exception.ResourceNotFoundException;
import com.zjsu.pjt.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.List;
import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductService productService;

    // --- 对外展示接口 ---

    @GetMapping
    @Operation(summary = "获取所有上架商品")
    public List<Product> getOnShelfProducts() {
        return productRepository.findByOnShelfTrue();
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "按分类展示上架商品")
    public List<Product> getProductsByCategory(@PathVariable String category) {
        return productRepository.findByCategoryAndOnShelfTrue(category);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取单个商品详情")
    public ResponseEntity<Product> getProductById(@PathVariable UUID id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }



    // --- 新增、修改、删除接口 ---

    @PostMapping
    @Operation(summary = "新增商品")
    @ResponseStatus(HttpStatus.CREATED) // 返回 201 Created 状态码
    public Product createProduct(@Valid @RequestBody Product product) {
        // @Valid 注解会触发实体类中的验证规则
        return productService.createProduct(product);

    }

    @PutMapping("/{id}")
    @Operation(summary = "修改商品信息")
    public ResponseEntity<Product> updateProduct(@PathVariable UUID id, @Valid @RequestBody Product productDetails) {
            Product updatedProduct = productService.updateProduct(id, productDetails);
            return ResponseEntity.ok(updatedProduct);
        }


    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        boolean deleted = productService.deleteProduct(id);
        return ResponseEntity.noContent().build(); // 返回 204 No Content 表示成功删除

    }


    // --- 内部服务调用接口 (供Order-Service调用) ---

    @PostMapping("/internal/deduct-stock")
    @Operation(summary = "扣减库存（内部接口）")
    public ResponseEntity<Void> deductStock(@RequestParam UUID productId, @RequestParam int quantity) {
            productService.deductStock(productId, quantity);
            return ResponseEntity.ok().build();

    }

    @PostMapping("/internal/increase-stock")
    @Operation(summary = "增加库存（内部接口）")
    public ResponseEntity<Void> increaseStock(@RequestParam UUID productId, @RequestParam int quantity) {
            productService.increaseStock(productId, quantity);
            return ResponseEntity.ok().build();

    }


}
