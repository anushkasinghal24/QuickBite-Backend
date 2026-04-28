package com.quickbite.order.service;

import com.quickbite.order.dto.*;
import com.quickbite.order.entity.Order.OrderStatus;

import java.util.List;

/**
 * OrderService Interface
 *
 * Declares all order operations as per PDF Section 4.5:
 *  placeOrder(), getOrderById(), getOrdersByCustomer(),
 *  getOrdersByRestaurant(), getActiveOrders(), updateStatus(),
 *  assignDeliveryAgent(), cancelOrder(), reorderFromHistory(), getOrderCount()
 *
 * Additional methods for analytics and admin support.
 */
public interface OrderService {

    // â”€â”€ Core Order Operations â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Place a new order from the customer's current cart.
     * Orchestrates: cart snapshot â†’ payment â†’ cart clear â†’ agent assign â†’ notify
     */
    OrderResponse placeOrder(int customerId, String customerName, PlaceOrderRequest request);

    /**
     * Fetch a single order by orderId.
     * Used for order detail view and tracking screen.
     */
    OrderResponse getOrderById(int orderId);

    // â”€â”€ Customer Queries â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** All orders for a customer (order history â€” PDF Section 2.2) */
    List<OrderSummaryDTO> getOrdersByCustomer(int customerId);

    /** In-flight orders for customer tracking screen */
    List<OrderResponse> getActiveOrdersByCustomer(int customerId);

    // â”€â”€ Restaurant Queries â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** All orders for a restaurant's dashboard */
    List<OrderSummaryDTO> getOrdersByRestaurant(int restaurantId);

    // â”€â”€ Delivery Agent Queries â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** All orders assigned to a delivery agent */
    List<OrderSummaryDTO> getOrdersByAgent(int agentId);

    // â”€â”€ Admin Queries â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** All orders platform-wide (admin monitoring â€” PDF Section 2.5) */
    List<OrderSummaryDTO> getAllOrders();

    /** All active (non-terminal) orders platform-wide */
    List<OrderSummaryDTO> getAllActiveOrders();

    // â”€â”€ Status Management â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Advance order through lifecycle.
     * Restaurant: PLACEDâ†’CONFIRMEDâ†’PREPARING
     * Agent:      CONFIRMEDâ†’PICKED_UPâ†’DELIVERED
     * Admin:      any transition
     */
    OrderResponse updateStatus(int orderId, OrderStatus newStatus, String callerRole);

    /**
     * Assign a delivery agent to an order.
     * Called by admin or auto-assignment logic after order is CONFIRMED.
     */
    OrderResponse assignDeliveryAgent(int orderId, int agentId);

    /**
     * Cancel an order.
     * Only allowed before PREPARING begins (PDF Section 2.2).
     * Triggers refund if payment was already processed.
     */
    OrderResponse cancelOrder(int orderId, int customerId, boolean isAdmin);

    /**
     * Recreate a cart from a past order with one click.
     * Returns the new Order after placing it, or just an OrderResponse
     * showing the cart was repopulated (depends on UX choice).
     * PDF Section 2.2: "Reorder from any past order in one click."
     */
    OrderResponse reorderFromHistory(int orderId, int customerId, PlaceOrderRequest request);

    // â”€â”€ Analytics â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Revenue + order count analytics for a restaurant.
     * PDF Section 2.3: "daily/weekly/monthly revenue, top-selling items"
     */
    RevenueAnalyticsDTO getRevenueAnalytics(int restaurantId);

    /**
     * Total order count for a restaurant.
     * PDF Section 4.5: getOrderCount()
     */
    long getOrderCount(int restaurantId);
}
