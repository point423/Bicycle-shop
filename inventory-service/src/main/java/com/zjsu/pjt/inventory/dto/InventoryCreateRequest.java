package com.zjsu.pjt.inventory.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class InventoryCreateRequest {
    private UUID productId;
    private Integer stock; // 接收初始库存
}
