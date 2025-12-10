package com.zjsu.pjt.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "inventory")
@Data
@NoArgsConstructor
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 主键

    @Column(unique = true, nullable = false)
    private UUID productId; // 关联的商品ID，必须唯一

    @Column(nullable = false)
    private Integer stock; // 库存数量

    @Version // 乐观锁版本号，用于防止超卖
    private Integer version;

    @Column(nullable = false)
    private boolean onShelf = false; // 上架状态，默认为false（下架）

    public Inventory(UUID productId, Integer stock) {
        this.productId = productId;
        this.stock = stock;
        this.onShelf = false; // 创建时默认为下架
    }
}
