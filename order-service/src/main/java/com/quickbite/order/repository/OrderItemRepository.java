package com.quickbite.order.repository;

import com.quickbite.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

/**
 * OrderItemRepository
 *
 * Manages immutable order item snapshots.
 * Used for reorder functionality and top-selling items analytics.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    /** All items for a specific order */
    List<OrderItem> findByOrderOrderId(int orderId);

    /**
     * Top-selling menu items for a restaurant.
     * Returns [menuItemId, itemName, totalQuantitySold] sorted by quantity desc.
     * Used for restaurant earnings analytics (PDF Section 2.3).
     */
    @Query("SELECT oi.menuItemId, oi.name, SUM(oi.quantity) as totalSold " +
           "FROM OrderItem oi JOIN oi.order o " +
           "WHERE o.restaurantId = :restaurantId AND o.orderStatus = 'DELIVERED' " +
           "GROUP BY oi.menuItemId, oi.name " +
           "ORDER BY totalSold DESC")
    List<Object[]> findTopSellingItemsByRestaurant(@Param("restaurantId") int restaurantId);

    /**
     * Top-selling menu items for a restaurant within a date range.
     * Used for revenue analytics.
     */
    @Query("SELECT oi.menuItemId, oi.name, SUM(oi.quantity) as totalSold, SUM(oi.price * oi.quantity) as totalRevenue " +
           "FROM OrderItem oi JOIN oi.order o " +
           "WHERE o.restaurantId = :restaurantId " +
           "AND o.orderStatus = 'DELIVERED' " +
           "AND o.orderDate BETWEEN :start AND :end " +
           "GROUP BY oi.menuItemId, oi.name " +
           "ORDER BY totalSold DESC")
    List<Object[]> findTopSellingItemsByRestaurantAndDateRange(
            @Param("restaurantId") int restaurantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
