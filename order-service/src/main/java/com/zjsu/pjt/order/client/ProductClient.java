package com.zjsu.pjt.order.client;

import com.zjsu.pjt.order.dto.ProductClientDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

// 注意：这里只定义接口
@FeignClient(name = "product-service", fallback = ProductClientFallback.class)
public interface ProductClient {
    @GetMapping("/api/products/{id}")
    ResponseEntity<ProductClientDTO> getProductById(@PathVariable("id") UUID id);
}