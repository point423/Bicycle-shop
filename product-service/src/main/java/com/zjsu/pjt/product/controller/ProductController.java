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
    public ResponseEntity<java.lang.Void> deleteProduct(@PathVariable UUID id) {
        boolean deleted = productService.deleteProduct(id);
        if (deleted) {
            return ResponseEntity.noContent().build(); // 204 No Content
        } else {
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
    }
}
