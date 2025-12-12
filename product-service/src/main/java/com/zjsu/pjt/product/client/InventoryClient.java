package com.zjsu.pjt.product.client;

import com.zjsu.pjt.product.dto.InventoryCreateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "inventory-service", fallback = InventoryClientFallback.class)
public interface InventoryClient {

    @PostMapping("/api/inventory/create")
    ResponseEntity<Map<String, Object>> createInventoryRecord(@RequestBody InventoryCreateRequest request);

    @DeleteMapping("/api/inventory/{productId}")
    void deleteInventoryRecord(@PathVariable("productId") UUID productId);

    @PutMapping("/api/inventory/{productId}/on-shelf")
    ResponseEntity<Map<String, Object>> updateOnShelfStatus(@PathVariable("productId") UUID productId, @RequestParam("onShelf") boolean onShelf);

    @GetMapping("/api/inventory/on-shelf-product-ids")
    ResponseEntity<Map<String, Object>> getOnShelfProductIds();
}
