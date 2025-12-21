package com.zjsu.pjt.product.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * 产品实体类，用于描述自行车商城的商品信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "产品实体类")
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_brand_model", columnList = "brand, model")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "产品ID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef", readOnly = true)
    private UUID id;

    @Column(nullable = false)
    @Schema(description = "品牌", example = "Giant", required = true)
    @NotBlank(message = "品牌不能为空")
    private String brand; // 品牌

    @Column(nullable = false)
    @Schema(description = "分类（如：山地车、公路车、城市车）", example = "公路车", required = true)
    @NotBlank(message = "分类不能为空")
    private String category; // 分类

    @Column(nullable = false)
    @Schema(description = "型号", example = "TCR Advanced 3", required = true)
    @NotBlank(message = "型号不能为空")
    private String model; // 型号

    @Schema(description = "变速系统", example = "Shimano 105")
    private String gearSystem; // 变速系统

    @Schema(description = "车架尺寸", example = "M")
    private String frameSize; // 车架尺寸

    @Schema(description = "颜色", example = "黑")
    private String color; // 颜色

    @Column(nullable = false)
    @Schema(description = "价格（单位：元）", example = "15000", required = true)
    @Min(value = 1000, message = "价格必须大于1000")
    private Integer price; // 价格

    @Schema(description = "产品图片URL", example = "/images/giant-tcr.jpg")
    private String imageUrl; // 新增产品图片URL字段



    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // 系统生成创建时间

}
