package com.zjsu.pjt.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * 创建库存记录的请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCreateRequest {
    private UUID productId;
    private Integer stock;
}
