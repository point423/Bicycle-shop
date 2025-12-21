package com.zjsu.pjt.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryUpdateRequest {
    private UUID productId;
    private Integer quantity;
}