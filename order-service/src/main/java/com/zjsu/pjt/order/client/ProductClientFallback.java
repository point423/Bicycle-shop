package com.zjsu.pjt.order.client;

import com.zjsu.pjt.order.dto.ProductClientDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * 商品服务降级处理类
 * 必须添加 @Component 注解交给 Spring 管理
 */
@Component
public class ProductClientFallback implements ProductClient {

    @Override
    public ResponseEntity<ProductClientDTO> getProductById(UUID id) {
        // 当调用失败时，打印日志
        System.err.println("Product Service is unavailable. Fallback for product id: " + id);

        // 返回一个空的 DTO 或者特定的错误标识
        ProductClientDTO fallbackDTO = new ProductClientDTO();
        fallbackDTO.setId(id);
        fallbackDTO.setModel("未知商品(服务暂时不可用)");
        fallbackDTO.setBrand("-");
        fallbackDTO.setPrice(0);

        // 返回 503 Service Unavailable 或者 200 OK 带着空数据，视业务需求而定
        // 这里返回 200 OK 但数据为空，保证前端不报错
        return ResponseEntity.ok(fallbackDTO);
    }
}