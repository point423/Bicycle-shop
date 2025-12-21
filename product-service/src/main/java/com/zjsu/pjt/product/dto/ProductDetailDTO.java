package com.zjsu.pjt.product.dto;

import com.zjsu.pjt.product.model.Product;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ProductDetailDTO {

    private UUID id;
    private String brand;
    private String category;
    private String model;
    private String gearSystem;
    private String frameSize;
    private String color;
    private Integer price;
    private LocalDateTime createdAt;
    private String imageUrl;
    private Integer stock; // 库存字段

    // 构造函数，用于从 Product 和 stock 组装
    public ProductDetailDTO(Product product, Integer stock) {
        this.id = product.getId();
        this.brand = product.getBrand();
        this.category = product.getCategory();
        this.model = product.getModel();
        this.gearSystem = product.getGearSystem();
        this.frameSize = product.getFrameSize();
        this.color = product.getColor();
        this.price = product.getPrice();
        this.createdAt = product.getCreatedAt();
        this.imageUrl = product.getImageUrl();
        this.stock = stock;
    }
}