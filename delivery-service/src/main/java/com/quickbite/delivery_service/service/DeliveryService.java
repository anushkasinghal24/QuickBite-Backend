package com.quickbite.delivery_service.service;

import com.quickbite.delivery_service.dto.*;
import com.quickbite.delivery_service.entity.DeliveryAgent;

import java.util.List;

/**
 * DeliveryService Interface
 *
 * PDF Section 4.7 exact methods:
 *  registerAgent(), getAgentById(), getAgentByUserId(), getNearbyAgents(),
 *  updateLocation(), setAvailability(), verifyAgent(),
 *  assignOrder(), getAssignedOrder(), pickUpOrder(), completeDelivery(),
 *  updateRating(), getActiveDeliveries()
 */
public interface DeliveryService {

    /**
     * Register new delivery agent.
     * PDF: "Register as a delivery agent with personal details, vehicle type, vehicle reg number."
     * Status starts as PENDING — admin must verify before agent can receive orders.
     */
    AgentResponse registerAgent(Integer userId, RegisterAgentRequest request);

    /**
     * Get agent profile by agentId.
     */
    AgentResponse getAgentById(Integer agentId);

    /**
     * Get agent profile by userId (from JWT).
     * Used by agent to view their own profile.
     */
    AgentResponse getAgentByUserId(Integer userId);

    /**
     * Find available + verified agents near given GPS coordinates.
     * PDF: "geolocation-based query to find available agents near a restaurant"
     * Uses Haversine formula — PDF NFR Section 6.
     */
    List<AgentResponse> getNearbyAgents(Double latitude, Double longitude, Double radiusKm);

    /**
     * Update agent's live GPS location.
     * PDF: "Update live GPS coordinates at configurable intervals for real-time tracking."
     * PDF NFR: "WebSocket (STOMP) via Spring WebSocket every 15 seconds."
     */
    AgentResponse updateLocation(Integer agentId, UpdateLocationRequest request);

    /**
     * Toggle agent availability (online/offline).
     * PDF: "Toggle availability (online/offline) to start or stop receiving orders."
     */
    AgentResponse setAvailability(Integer agentId, SetAvailabilityRequest request);

    /**
     * Admin: verify, reject, or suspend an agent.
     * PDF: "Verify delivery agent identity and vehicle documents before activating account."
     */
    AgentResponse verifyAgent(Integer agentId, VerifyAgentRequest request);

    /**
     * Assign an order to this agent.
     * Called internally by order-service when placing an order.
     * Sets currentOrderId and marks agent as busy (isAvailable stays true,
     * but currentOrderId != null means occupied).
     */
    AgentResponse assignOrder(Integer agentId, AssignOrderRequest request);

    /**
     * View the currently assigned order with pickup and delivery addresses.
     */
    AssignedOrderResponse getAssignedOrder(Integer agentId);

    /**
     * Mark an assigned order as picked up from the restaurant.
     */
    AgentResponse pickUpOrder(Integer agentId, Integer orderId);

    /**
     * Mark delivery as complete.
     * PDF: "Mark orders as picked up from restaurant and delivered to customer."
     * Clears currentOrderId, increments totalDeliveries.
     * Earnings per delivery calculated here.
     */
    AgentResponse completeDelivery(Integer agentId, Integer orderId);

    /**
     * Get completed delivery history for the agent.
     */
    List<DeliveryHistoryResponse> getDeliveryHistory(Integer agentId);

    /**
     * Update agent's average rating.
     * Called by review-service after customer submits delivery rating.
     * PDF Section 4.8: "Average ratings pushed back to Delivery-Agent-Service."
     */
    AgentResponse updateRating(Integer agentId, UpdateRatingRequest request);

    /**
     * Get all agents with active deliveries (currentOrderId != null).
     * PDF: getActiveDeliveries()
     */
    List<AgentResponse> getActiveDeliveries();

    /**
     * Get earnings + stats summary for agent.
     * PDF: "View earnings summary and customer delivery ratings."
     */
    EarningsSummaryResponse getEarningsSummary(Integer agentId);

    /**
     * Get all agents — ADMIN use.
     */
    List<AgentResponse> getAllAgents();

    /**
     * Get agents by status — ADMIN use (PENDING/VERIFIED/SUSPENDED).
     */
    List<AgentResponse> getAgentsByStatus(DeliveryAgent.AgentStatus status);

    /**
     * Get agent's live location only — for customer order tracking screen.
     */
    AgentLocationResponse getAgentLocation(Integer agentId);

    /**
     * Delete agent — ADMIN use.
     * PDF Section 4.7: deleteByAgentId()
     */
    void deleteAgent(Integer agentId);
}
