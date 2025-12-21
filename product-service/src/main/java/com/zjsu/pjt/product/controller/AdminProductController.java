package com.zjsu.pjt.product.controller;

import com.zjsu.pjt.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.zjsu.pjt.product.dto.ProductDetailDTO;
import com.zjsu.pjt.product.model.Product;



import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products")
@Tag(name = "Admin Product Management", description = "供管理员使用的商品管理API")
public class AdminProductController {

    @Autowired
    private ProductService productService;

    // --- 新增：获取所有商品（无论是否上架） ---
    @GetMapping("/all")
    @Operation(summary = "获取所有商品列表（无论是否上架）")
    public ResponseEntity<List<ProductDetailDTO>> getAllProducts() {
        List<ProductDetailDTO> allProducts = productService.findAllProductsWithStock();
        return ResponseEntity.ok(allProducts);
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "上架指定商品")
    public ResponseEntity<Void> publishProduct(@PathVariable UUID id) {
        productService.updateOnShelfStatus(id, true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/unpublish")
    @Operation(summary = "下架指定商品")
    public ResponseEntity<Void> unpublishProduct(@PathVariable UUID id) {
        productService.updateOnShelfStatus(id, false);
        return ResponseEntity.ok().build();
    }

    // --- 补全：修改商品信息 ---
    @PutMapping("/{id}")
    @Operation(summary = "管理员修改商品信息")
    public ResponseEntity<Product> updateProduct(@PathVariable UUID id, @RequestBody Product productDetails) {
        Product updatedProduct = productService.updateProduct(id, productDetails);
        return ResponseEntity.ok(updatedProduct);
    }

    // --- 补全：删除商品 ---
    @DeleteMapping("/{id}")
    @Operation(summary = "管理员强制删除商品")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}