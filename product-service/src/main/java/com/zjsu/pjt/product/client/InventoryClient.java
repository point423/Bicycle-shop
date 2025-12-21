package com.zjsu.pjt.product.client;

import com.zjsu.pjt.product.dto.InventoryCreateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping; // 导入
import org.springframework.web.bind.annotation.RequestBody; // 导入
import java.util.List;
import com.zjsu.pjt.product.dto.InventoryUpdateRequest; // 确保你创建了这个DTO


@FeignClient(name = "inventory-service", fallback = InventoryClientFallback.class)
public interface InventoryClient {

    @PostMapping("/api/inventorys/create")
    ResponseEntity<Map<String, Object>> createInventoryRecord(@RequestBody InventoryCreateRequest request);

    @DeleteMapping("/api/inventorys/{productId}")
    void deleteInventoryRecord(@PathVariable("productId") UUID productId);

    @PutMapping("/api/inventorys/{productId}/on-shelf")
    ResponseEntity<Map<String, Object>> updateOnShelfStatus(@PathVariable("productId") UUID productId, @RequestParam("onShelf") boolean onShelf);

    @GetMapping("/api/inventorys/on-shelf-product-ids")
    ResponseEntity<Map<String, Object>> getOnShelfProductIds();


    @PostMapping("/api/inventorys/decrease")
    ResponseEntity<Map<String, Object>> decreaseStock(@RequestBody InventoryUpdateRequest request);

    @PostMapping("/api/inventorys/increase")
    ResponseEntity<Map<String, Object>> increaseStock(@RequestBody InventoryUpdateRequest request);


    @PostMapping("/api/inventorys/stocks")
    Map<UUID, Integer> getStocksByProductIds(@RequestBody List<UUID> productIds);

}
