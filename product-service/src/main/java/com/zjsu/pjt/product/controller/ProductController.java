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
        return productService.findOnShelfProducts();    }

    @GetMapping("/category/{category}")
    @Operation(summary = "按分类展示上架商品")
    public List<Product> getProductsByCategory(@PathVariable String category) {
        return productService.findOnShelfProductsByCategory(category);    }

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
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(@Valid @RequestBody Product product) {
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
        // 直接调用 service 方法。
        // 如果找不到商品，service 会抛出 ResourceNotFoundException，
        // 全局异常处理器会将其转换为 404 响应。
        // 如果调用库存服务失败，会抛出 BusinessException，转换为 500 响应。
        // 如果方法正常结束，说明删除成功。
        productService.deleteProduct(id);

        // 方法能执行到这里，就直接返回 204 No Content
        return ResponseEntity.noContent().build();
    }
}
