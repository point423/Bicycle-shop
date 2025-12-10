package com.zjsu.pjt.product.controller;

import com.zjsu.pjt.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products") // 使用 /api/admin 前缀
@Tag(name = "Admin Product Management", description = "供管理员使用的商品管理API")
public class AdminProductController {

    @Autowired
    private ProductService productService;

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

    // 将来还可以添加其他管理功能，例如：
    // GET / - 查看所有商品（无论是否上架）
    // PUT /{id} - 管理员修改商品信息
    // DELETE /{id} - 管理员强制删除商品
}
