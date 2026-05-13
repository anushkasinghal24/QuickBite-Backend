package com.quickbite.order.repository;

import com.quickbite.order.entity.Order;
import com.quickbite.order.entity.Order.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * OrderRepository
 *
 * As per PDF Section 4.5:
 *  findByCustomerId(), findByRestaurantId(), findByOrderStatus(),
 *  findByDeliveryAgentId(), findByOrderId(), findByOrderDateBetween(),
 *  countByRestaurantId()
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // â”€â”€ By Customer â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** All orders placed by a specific customer (order history) */
    List<Order> findByCustomerIdOrderByOrderDateDesc(int customerId);

    /** Find a specific order by orderId â€” used in cancel/track */
    Optional<Order> findByOrderIdAndCustomerId(int orderId, int customerId);

    // â”€â”€ By Restaurant â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** All orders for a restaurant's dashboard (incoming orders queue) */
    List<Order> findByRestaurantIdOrderByOrderDateDesc(int restaurantId);

    /** Active (non-terminal) orders for a restaurant */
    List<Order> findByRestaurantIdAndOrderStatusNotInOrderByOrderDateAsc(
            int restaurantId, List<OrderStatus> terminalStatuses);

    // â”€â”€ By Status â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Orders by a specific lifecycle status (platform-wide) */
    List<Order> findByOrderStatusOrderByOrderDateDesc(OrderStatus orderStatus);

    // â”€â”€ By Delivery Agent â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** All orders assigned to a delivery agent */
    List<Order> findByDeliveryAgentIdOrderByOrderDateDesc(int deliveryAgentId);

    /** Currently active delivery for an agent */
    List<Order> findByDeliveryAgentIdAndOrderStatusIn(
            int deliveryAgentId, List<OrderStatus> statuses);

    // â”€â”€ Date Range â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Orders placed within a date range (for analytics / admin) */
    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    /** Orders for a restaurant within a date range (earnings analytics) */
    List<Order> findByRestaurantIdAndOrderDateBetween(
            int restaurantId, LocalDateTime start, LocalDateTime end);

    // â”€â”€ Counts â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Total order count for a restaurant (PDF: countByRestaurantId) */
    long countByRestaurantId(int restaurantId);

    /** Count of orders by status for a customer */
    long countByCustomerIdAndOrderStatus(int customerId, OrderStatus status);

    // â”€â”€ Active Orders â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Active orders â€” all orders that are NOT in a terminal state.
     * Used by admin dashboard.
     */
    @Query("SELECT o FROM Order o WHERE o.orderStatus NOT IN " +
           "('DELIVERED', 'CANCELLED') ORDER BY o.orderDate DESC")
    List<Order> findAllActiveOrders();

    /**
     * Active orders for a specific customer (in-flight orders for tracking screen).
     */
    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId " +
           "AND o.orderStatus NOT IN ('DELIVERED', 'CANCELLED') " +
           "ORDER BY o.orderDate DESC")
    List<Order> findActiveOrdersByCustomer(@Param("customerId") int customerId);

    // â”€â”€ Revenue Analytics â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Sum of finalAmount for delivered orders of a restaurant in a date range.
     * Used for restaurant earnings analytics (PDF Section 2.3).
     */
    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o " +
           "WHERE o.restaurantId = :restaurantId " +
           "AND o.orderStatus = 'DELIVERED' " +
           "AND o.orderDate BETWEEN :start AND :end")
    double sumRevenueByRestaurantAndDateRange(
            @Param("restaurantId") int restaurantId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Platform-wide revenue (all delivered orders).
     * Used by Admin analytics (PDF Section 2.5).
     */
    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o " +
           "WHERE o.orderStatus = 'DELIVERED'")
    double sumTotalPlatformRevenue();

    /**
     * Order count grouped by restaurantId for a date range.
     * Used in admin analytics to find peak zones.
     */
    @Query("SELECT o.restaurantId, COUNT(o) FROM Order o " +
           "WHERE o.orderDate BETWEEN :start AND :end " +
           "GROUP BY o.restaurantId ORDER BY COUNT(o) DESC")
    List<Object[]> countOrdersGroupedByRestaurant(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
