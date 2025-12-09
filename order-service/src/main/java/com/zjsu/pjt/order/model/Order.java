package com.zjsu.pjt.order.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 订单实体类，记录用户的购买信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单实体类")
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_buyer_id", columnList = "buyerId"),
        @Index(name = "idx_product_id", columnList = "productId")
})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "订单ID", example = "f0e9d8c7-b6a5-4321-fedc-ba9876543210", readOnly = true)
    private UUID id;

    @Column(nullable = false)
    @Schema(description = "所购买的产品ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef", required = true)
    private UUID productId; // 产品ID

    @Column(nullable = false)
    @Schema(description = "购买者（用户）的ID", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
    private UUID buyerId; // 购买者ID

    @Column(nullable = false)
    @Schema(description = "订单状态", example = "active", required = true, defaultValue = "active")
    private String status = "active"; // 订单状态，默认为 'active'

    @Column(nullable = false)
    @Schema(description = "订单购买数量", example = "3", required = true)
    private Integer quantity; // 订单购买数量

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @Schema(description = "订单创建时间（系统自动生成）", example = "2024-10-01T10:30:00", readOnly = true)
    private LocalDateTime createdAt; // 系统生成创建时间
}
