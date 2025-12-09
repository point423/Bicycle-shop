package com.zjsu.pjt.order.repository;

import com.zjsu.pjt.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 订单数据访问接口
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * 根据购买者ID查询订单列表，并按创建时间降序排列
     * @param buyerId 购买者ID
     * @return 订单列表
     */
    List<Order> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);

    /**
     * 根据订单状态查询订单列表
     * @param status 订单状态
     * @return 订单列表
     */
    List<Order> findByStatus(String status);

    /**
     * 根据购买者ID和订单状态查询订单列表
     * @param buyerId 购买者ID
     * @param status 订单状态
     * @return 订单列表
     */
    List<Order> findByBuyerIdAndStatus(UUID buyerId, String status);

    /**
     * 查询在指定时间范围内的所有订单
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 订单列表
     */
    List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}

