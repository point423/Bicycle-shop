package com.zjsu.pjt.inventory.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class InventoryUpdateRequest {
    private UUID productId;
    private Integer quantity;
}
