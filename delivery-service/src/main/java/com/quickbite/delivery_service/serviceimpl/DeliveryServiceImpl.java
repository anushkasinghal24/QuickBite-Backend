package com.quickbite.delivery_service.serviceimpl;

import com.quickbite.delivery_service.dto.*;
import com.quickbite.delivery_service.entity.DeliveryAgent;
import com.quickbite.delivery_service.entity.DeliveryHistory;
import com.quickbite.delivery_service.exception.*;
import com.quickbite.delivery_service.feign.OrderServiceClient;
import com.quickbite.delivery_service.feign.NotificationServiceClient;
import com.quickbite.delivery_service.feign.RestaurantServiceClient;
import com.quickbite.delivery_service.repository.DeliveryHistoryRepository;
import com.quickbite.delivery_service.repository.DeliveryRepository;
import com.quickbite.delivery_service.service.DeliveryService;
import com.quickbite.delivery_service.service.RabbitNotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DeliveryServiceImpl
 *
 * Complete business logic for Delivery Agent management.
 * PDF Section 4.7 — all methods implemented.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryHistoryRepository deliveryHistoryRepository;
    private final NotificationServiceClient notificationServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;
    private final RabbitNotificationPublisher rabbitNotificationPublisher;

    /** Earnings per delivery in ₹ (configurable — move to application.yml later) */
    private static final double EARNINGS_PER_DELIVERY = 50.0;
    /** Default search radius in km */
    private static final double DEFAULT_RADIUS_KM = 5.0;

    // ═══════════════════════════════════════════════════════════════════
    // 1. REGISTER AGENT
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public AgentResponse registerAgent(Integer userId, RegisterAgentRequest request) {
        log.info("Registering delivery agent for userId={}", userId);

        // Prevent duplicate registration
        if (deliveryRepository.existsByUserId(userId)) {
            throw new DuplicateAgentException(
                "A delivery agent profile already exists for userId: " + userId);
        }

        DeliveryAgent agent = DeliveryAgent.builder()
                .userId(userId)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .vehicleType(request.getVehicleType())
                .vehicleNumber(request.getVehicleNumber())
                .isAvailable(false)      // Offline by default until admin verifies
                .isVerified(false)
                .status(DeliveryAgent.AgentStatus.PENDING)
                .avgRating(0.0)
                .totalDeliveries(0)
                .totalEarnings(0.0)
                .build();

        DeliveryAgent saved = deliveryRepository.save(agent);
        log.info("Delivery agent registered: agentId={}, status=PENDING", saved.getAgentId());

        // Notify admin about new agent registration (fallback handles if notif-service is down)
        sendNotification(
            "ADMIN_NOTIFICATION",
            0,  // Admin userId placeholder — notification-service routes to all admins
            "New Agent Registration",
            "Delivery agent " + request.getFullName() + " has registered. Please verify.",
            saved.getAgentId()
        );

        return AgentResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryHistoryResponse> getDeliveryHistory(Integer agentId) {
        findAgentById(agentId);
        return deliveryHistoryRepository.findByAgentIdOrderByDeliveredAtDesc(agentId)
                .stream()
                .map(DeliveryHistoryResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AssignedOrderResponse getAssignedOrder(Integer agentId) {
        DeliveryAgent agent = findAgentById(agentId);
        if (agent.getCurrentOrderId() == null) {
            throw new InvalidDeliveryException("No active order is assigned to agentId=" + agentId);
        }

        OrderDetailsDTO order = fetchAssignedOrder(agent.getCurrentOrderId());
        RestaurantDetailsDTO restaurant = fetchRestaurantDetails(order.getRestaurantId().longValue());
        return AssignedOrderResponse.from(agent.getCurrentOrderId(), agent, order, restaurant);
    }

    @Override
    public AgentResponse pickUpOrder(Integer agentId, Integer orderId) {
        DeliveryAgent agent = findAgentById(agentId);
        if (agent.getCurrentOrderId() == null || !agent.getCurrentOrderId().equals(orderId)) {
            throw new InvalidDeliveryException(
                "Agent agentId=" + agentId + " does not have order #" + orderId + " assigned.");
        }

        OrderDetailsDTO order = fetchAssignedOrder(orderId);
        if (!"READY_TO_PICK_UP".equalsIgnoreCase(order.getOrderStatus())) {
            throw new InvalidDeliveryException(
                "Order #" + orderId + " is not ready for pickup yet. Current status: " + order.getOrderStatus());
        }

        orderServiceClient.updateOrderStatus(orderId, Map.of("status", "PICKED_UP"));
        log.info("Order #{} marked as PICKED_UP by agentId={}", orderId, agentId);
        return AgentResponse.from(agent);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 2. GET AGENT BY ID
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public AgentResponse getAgentById(Integer agentId) {
        DeliveryAgent agent = findAgentById(agentId);
        return AgentResponse.from(agent);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 3. GET AGENT BY USER ID
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public AgentResponse getAgentByUserId(Integer userId) {
        DeliveryAgent agent = deliveryRepository.findByUserId(userId)
                .orElseThrow(() -> new AgentNotFoundException(
                    "No delivery agent profile found for userId: " + userId));
        return AgentResponse.from(agent);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 4. GET NEARBY AGENTS (Haversine)
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<AgentResponse> getNearbyAgents(Double latitude, Double longitude, Double radiusKm) {
        double radius = (radiusKm != null && radiusKm > 0) ? radiusKm : DEFAULT_RADIUS_KM;
        log.info("Finding nearby agents: lat={}, lng={}, radius={}km", latitude, longitude, radius);

        List<DeliveryAgent> agents = deliveryRepository.findNearbyAgents(latitude, longitude, radius);
        log.info("Found {} nearby eligible agents", agents.size());

        return agents.stream().map(AgentResponse::from).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════
    // 5. UPDATE LIVE LOCATION
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public AgentResponse updateLocation(Integer agentId, UpdateLocationRequest request) {
        DeliveryAgent agent = findAgentById(agentId);

        agent.setCurrentLatitude(request.getLatitude());
        agent.setCurrentLongitude(request.getLongitude());
        agent.setLocationUpdatedAt(LocalDateTime.now());

        DeliveryAgent saved = deliveryRepository.save(agent);
        log.debug("Location updated for agentId={}: lat={}, lng={}",
                agentId, request.getLatitude(), request.getLongitude());

        return AgentResponse.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 6. SET AVAILABILITY (ONLINE / OFFLINE)
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public AgentResponse setAvailability(Integer agentId, SetAvailabilityRequest request) {
        DeliveryAgent agent = findAgentById(agentId);

        // Agent can only go online if they are verified
        if (Boolean.TRUE.equals(request.getAvailable())
                && !Boolean.TRUE.equals(agent.getIsVerified())) {
            throw new AgentNotVerifiedException(
                "Agent must be verified by admin before going online. Current status: "
                        + agent.getStatus());
        }

        // Agent cannot go offline while actively delivering
        if (Boolean.FALSE.equals(request.getAvailable())
                && agent.getCurrentOrderId() != null) {
            throw new AgentBusyException(
                "Cannot go offline while delivering order #" + agent.getCurrentOrderId()
                + ". Please complete or hand over the delivery first.");
        }

        agent.setIsAvailable(request.getAvailable());
        DeliveryAgent saved = deliveryRepository.save(agent);

        log.info("Agent agentId={} is now {}", agentId,
                request.getAvailable() ? "ONLINE" : "OFFLINE");
        return AgentResponse.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 7. VERIFY AGENT (ADMIN)
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public AgentResponse verifyAgent(Integer agentId, VerifyAgentRequest request) {
        DeliveryAgent agent = findAgentById(agentId);

        switch (request.getAction().toUpperCase()) {

            case "VERIFY" -> {
                agent.setStatus(DeliveryAgent.AgentStatus.VERIFIED);
                agent.setIsVerified(true);
                agent.setAdminRemarks(null);
                log.info("Agent agentId={} VERIFIED by admin", agentId);

                // Notify agent they are now active
                sendNotification(
                    "AGENT_VERIFIED",
                    agent.getUserId(),
                    "Account Verified!",
                    "Your delivery agent account has been verified. You can now go online and start receiving orders.",
                    agentId
                );
            }

            case "REJECT" -> {
                agent.setStatus(DeliveryAgent.AgentStatus.REJECTED);
                agent.setIsVerified(false);
                agent.setIsAvailable(false);
                agent.setAdminRemarks(request.getRemarks());
                log.info("Agent agentId={} REJECTED by admin. Reason: {}", agentId, request.getRemarks());

                sendNotification(
                    "AGENT_REJECTED",
                    agent.getUserId(),
                    "Registration Rejected",
                    "Your delivery agent registration was rejected. Reason: " + request.getRemarks(),
                    agentId
                );
            }

            case "SUSPEND" -> {
                agent.setStatus(DeliveryAgent.AgentStatus.SUSPENDED);
                agent.setIsVerified(false);
                agent.setIsAvailable(false);
                agent.setAdminRemarks(request.getRemarks());
                log.info("Agent agentId={} SUSPENDED by admin. Reason: {}", agentId, request.getRemarks());

                sendNotification(
                    "AGENT_SUSPENDED",
                    agent.getUserId(),
                    "Account Suspended",
                    "Your delivery agent account has been suspended. Reason: " + request.getRemarks(),
                    agentId
                );
            }

            default -> throw new IllegalArgumentException(
                "Invalid action: " + request.getAction() + ". Use VERIFY, REJECT, or SUSPEND.");
        }

        DeliveryAgent saved = deliveryRepository.save(agent);
        return AgentResponse.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 8. ASSIGN ORDER
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public AgentResponse assignOrder(Integer agentId, AssignOrderRequest request) {
        DeliveryAgent agent = findAgentById(agentId);

        // Validate agent eligibility
        if (!agent.isEligibleForOrders()) {
            throw new AgentNotEligibleException(
                "Agent is not eligible for order assignment. " +
                "Status: " + agent.getStatus() +
                ", Available: " + agent.getIsAvailable() +
                ", CurrentOrder: " + agent.getCurrentOrderId());
        }

        agent.setCurrentOrderId(request.getOrderId());
        DeliveryAgent saved = deliveryRepository.save(agent);

        log.info("Order #{} assigned to agentId={}", request.getOrderId(), agentId);

        // Notify agent about new order assignment
        sendNotification(
            "ORDER_ASSIGNED",
            agent.getUserId(),
            "New Order Assigned",
            "You have been assigned order #" + request.getOrderId() + ". Please proceed to the restaurant for pickup.",
            request.getOrderId()
        );

        return AgentResponse.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 9. COMPLETE DELIVERY
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public AgentResponse completeDelivery(Integer agentId, Integer orderId) {
        DeliveryAgent agent = findAgentById(agentId);

        if (agent.getCurrentOrderId() == null
                || !agent.getCurrentOrderId().equals(orderId)) {
            throw new InvalidDeliveryException(
                "Agent agentId=" + agentId + " does not have order #" + orderId + " assigned.");
        }

        OrderDetailsDTO order = fetchAssignedOrder(orderId);
        RestaurantDetailsDTO restaurant = fetchRestaurantDetails(
                order.getRestaurantId() != null ? order.getRestaurantId().longValue() : null);

        agent.setCurrentOrderId(null);
        agent.setTotalDeliveries(agent.getTotalDeliveries() + 1);
        agent.setTotalEarnings(agent.getTotalEarnings() + EARNINGS_PER_DELIVERY);

        saveDeliveryHistory(agentId, order, restaurant);
        orderServiceClient.updateOrderStatus(orderId, Map.of("status", "DELIVERED"));
        DeliveryAgent saved = deliveryRepository.save(agent);
        log.info("Delivery complete — agentId={}, orderId={}, totalDeliveries={}",
                agentId, orderId, saved.getTotalDeliveries());

        // Notify agent about earnings
        sendNotification(
            "DELIVERY_COMPLETE",
            agent.getUserId(),
            "Delivery Completed!",
            "Order #" + orderId + " delivered. Earned ₹" + EARNINGS_PER_DELIVERY
            + ". Total deliveries: " + saved.getTotalDeliveries(),
            orderId
        );

        return AgentResponse.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 10. UPDATE RATING (called by review-service)
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public AgentResponse updateRating(Integer agentId, UpdateRatingRequest request) {
        DeliveryAgent agent = findAgentById(agentId);

        // Compute new rolling average:
        // newAvg = (currentAvg * totalDeliveries + newRating) / (totalDeliveries + 1)
        // But review-service sends already-computed avgRating — just update it.
        agent.setAvgRating(
            Math.round(request.getAvgRating() * 100.0) / 100.0
        );

        DeliveryAgent saved = deliveryRepository.save(agent);
        log.info("Rating updated for agentId={}: avgRating={}", agentId, saved.getAvgRating());
        return AgentResponse.from(saved);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 11. GET ACTIVE DELIVERIES
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<AgentResponse> getActiveDeliveries() {
        return deliveryRepository.findAll().stream()
                .filter(a -> a.getCurrentOrderId() != null)
                .map(AgentResponse::from)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════
    // 12. GET EARNINGS SUMMARY
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public EarningsSummaryResponse getEarningsSummary(Integer agentId) {
        DeliveryAgent agent = findAgentById(agentId);
        return EarningsSummaryResponse.from(agent);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 13. GET ALL AGENTS (ADMIN)
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<AgentResponse> getAllAgents() {
        return deliveryRepository.findAll().stream()
                .map(AgentResponse::from)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════
    // 14. GET AGENTS BY STATUS (ADMIN)
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<AgentResponse> getAgentsByStatus(DeliveryAgent.AgentStatus status) {
        return deliveryRepository.findByStatus(status).stream()
                .map(AgentResponse::from)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════
    // 15. GET AGENT LOCATION (customer tracking)
    // ═══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public AgentLocationResponse getAgentLocation(Integer agentId) {
        DeliveryAgent agent = findAgentById(agentId);
        return AgentLocationResponse.from(agent);
    }

    // ═══════════════════════════════════════════════════════════════════
    // 16. DELETE AGENT (ADMIN)
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public void deleteAgent(Integer agentId) {
        if (!deliveryRepository.existsById(agentId)) {
            throw new AgentNotFoundException("Agent not found with agentId: " + agentId);
        }
        deliveryRepository.deleteByAgentId(agentId);
        log.info("Agent deleted: agentId={}", agentId);
    }

    // ═══════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private DeliveryAgent findAgentById(Integer agentId) {
        return deliveryRepository.findByAgentId(agentId)
                .orElseThrow(() -> new AgentNotFoundException(
                    "Delivery agent not found with agentId: " + agentId));
    }

    private OrderDetailsDTO fetchAssignedOrder(Integer orderId) {
        try {
            ApiResponse<OrderDetailsDTO> response = orderServiceClient.getOrderById(orderId);
            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("order-service unavailable, using stub order {}: {}", orderId, e.getMessage());
        }

        return OrderDetailsDTO.builder()
                .orderId(orderId)
                .restaurantId(0)
                .restaurantName("Unknown restaurant")
                .deliveryAddress("Delivery address unavailable")
                .orderStatus("UNKNOWN")
                .finalAmount(0.0)
                .modeOfPayment("COD")
                .itemCount(0)
                .build();
    }

    private RestaurantDetailsDTO fetchRestaurantDetails(Long restaurantId) {
        try {
            if (restaurantId == null) {
                return RestaurantDetailsDTO.builder()
                        .restaurantId(0L)
                        .name("Unknown restaurant")
                        .address("Pickup address unavailable")
                        .build();
            }
            ApiResponse<RestaurantDetailsDTO> response = restaurantServiceClient.getRestaurantById(restaurantId);
            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("restaurant-service unavailable, using stub restaurant {}: {}", restaurantId, e.getMessage());
        }

        return RestaurantDetailsDTO.builder()
                .restaurantId(restaurantId)
                .name("Restaurant #" + restaurantId)
                .address("Pickup address unavailable")
                .build();
    }

    private void saveDeliveryHistory(Integer agentId,
                                     OrderDetailsDTO order,
                                     RestaurantDetailsDTO restaurant) {
        try {
            Integer orderId = order != null ? order.getOrderId() : null;
            if (orderId == null || deliveryHistoryRepository.existsByAgentIdAndOrderId(agentId, orderId)) {
                return;
            }

            DeliveryHistory history = DeliveryHistory.builder()
                    .agentId(agentId)
                    .orderId(orderId)
                    .customerId(order.getCustomerId())
                    .customerName(order.getCustomerName())
                    .restaurantId(order.getRestaurantId())
                    .restaurantName(order.getRestaurantName())
                    .pickupAddress(restaurant != null ? restaurant.getAddress() : null)
                    .deliveryAddress(order.getDeliveryAddress())
                    .finalAmount(order.getFinalAmount())
                    .modeOfPayment(order.getModeOfPayment())
                    .orderStatus("DELIVERED")
                    .itemCount(order.getItemCount())
                    .orderDate(order.getOrderDate())
                    .build();

            deliveryHistoryRepository.save(history);
        } catch (Exception e) {
            log.warn("Failed to save delivery history for agentId={}, orderId={}: {}",
                    agentId, order != null ? order.getOrderId() : null, e.getMessage());
        }
    }

    /**
     * Send notification via notification-service (Feign).
     * Graceful fallback — if notification-service is down, delivery still works.
     */
    private void sendNotification(String type, Integer recipientId,
                                   String title, String message, Integer relatedId) {
        try {
            Map<String, Object> payload = Map.of(
                    "type", type,
                    "recipientId", recipientId,
                    "title", title,
                    "message", message,
                    "relatedId", relatedId,
                    "relatedType", "DELIVERY_AGENT"
            );

            if (!rabbitNotificationPublisher.publish(payload)) {
                notificationServiceClient.sendNotification(payload);
            }
        } catch (Exception e) {
            // Non-blocking: notification failure must not break delivery service
            log.warn("Notification failed (non-critical): type={}, recipientId={} — {}",
                    type, recipientId, e.getMessage());
        }
    }
}
