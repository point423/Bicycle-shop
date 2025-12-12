package com.zjsu.pjt.order.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryUpdateRequest {
    private UUID productId;
    private Integer quantity;
}
