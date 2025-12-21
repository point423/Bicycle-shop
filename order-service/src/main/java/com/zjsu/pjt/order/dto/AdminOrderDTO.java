package com.zjsu.pjt.order.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AdminOrderDTO {
    private UUID id;            // 订单ID

    // --- 商品相关信息 ---
    private UUID productId;
    private String productBrand;    // 新增：品牌 (Giant)
    private String productModel;    // 新增：型号 (TCR Advanced 3)
    private String productCategory; // 新增：分类 (公路车)
    private Integer price;          // 新增：单价
    private String productImage;    // 图片URL (用于前端展示)

    // --- 买家相关信息 ---
    private UUID buyerId;
    private String buyerName;       // 买家名 (User.username)

    // --- 订单基础信息 ---
    private Integer quantity;       // 购买数量
    private String status;          // 订单状态
    private LocalDateTime createdAt;// 创建时间
}