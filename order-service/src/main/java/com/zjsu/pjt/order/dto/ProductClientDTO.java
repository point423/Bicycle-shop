package com.zjsu.pjt.order.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductClientDTO {
    private UUID id;
    private String brand;
    private String model;
    private String category;
    private Integer price;
    private String imageUrl;
}