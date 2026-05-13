package com.quickbite.delivery_service;

import com.quickbite.delivery_service.dto.*;
import com.quickbite.delivery_service.entity.DeliveryAgent;
import com.quickbite.delivery_service.exception.*;
import com.quickbite.delivery_service.feign.OrderServiceClient;
import com.quickbite.delivery_service.feign.NotificationServiceClient;
import com.quickbite.delivery_service.feign.RestaurantServiceClient;
import com.quickbite.delivery_service.repository.DeliveryRepository;
import com.quickbite.delivery_service.repository.DeliveryHistoryRepository;
import com.quickbite.delivery_service.service.RabbitNotificationPublisher;
import com.quickbite.delivery_service.serviceimpl.DeliveryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock private DeliveryRepository deliveryRepository;
    @Mock private NotificationServiceClient notificationServiceClient;
    @Mock private OrderServiceClient orderServiceClient;
    @Mock private RestaurantServiceClient restaurantServiceClient;
    @Mock private RabbitNotificationPublisher rabbitNotificationPublisher;
    @Mock private DeliveryHistoryRepository deliveryHistoryRepository;

    @InjectMocks private DeliveryServiceImpl deliveryService;

    private DeliveryAgent verifiedAgent;
    private DeliveryAgent pendingAgent;

    @BeforeEach
    void setUp() {
        verifiedAgent = DeliveryAgent.builder()
                .agentId(1)
                .userId(101)
                .fullName("Arjun Kumar")
                .phone("9876543210")
                .vehicleType(DeliveryAgent.VehicleType.BIKE)
                .vehicleNumber("DL01AB1234")
                .isAvailable(true)
                .isVerified(true)
                .status(DeliveryAgent.AgentStatus.VERIFIED)
                .currentOrderId(null)
                .avgRating(4.5)
                .totalDeliveries(50)
                .totalEarnings(2500.0)
                .build();

        pendingAgent = DeliveryAgent.builder()
                .agentId(2)
                .userId(102)
                .fullName("Raj Singh")
                .phone("9111222333")
                .vehicleType(DeliveryAgent.VehicleType.SCOOTER)
                .vehicleNumber("DL02CD5678")
                .isAvailable(false)
                .isVerified(false)
                .status(DeliveryAgent.AgentStatus.PENDING)
                .avgRating(0.0)
                .totalDeliveries(0)
                .totalEarnings(0.0)
                .build();
    }

    // ── Test: Register agent ─────────────────────────────────────────
    @Test
    void registerAgent_newUser_success() {
        when(deliveryRepository.existsByUserId(103)).thenReturn(false);
        when(deliveryRepository.save(any(DeliveryAgent.class))).thenReturn(pendingAgent);

        RegisterAgentRequest request = new RegisterAgentRequest(
            "New Agent", "9000000001",
            DeliveryAgent.VehicleType.BIKE, "DL03EF9999"
        );

        AgentResponse response = deliveryService.registerAgent(103, request);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        verify(deliveryRepository).save(any(DeliveryAgent.class));
    }

    // ── Test: Duplicate registration throws ──────────────────────────
    @Test
    void registerAgent_duplicateUserId_throwsDuplicateAgentException() {
        when(deliveryRepository.existsByUserId(101)).thenReturn(true);

        RegisterAgentRequest request = new RegisterAgentRequest(
            "Arjun Kumar", "9876543210",
            DeliveryAgent.VehicleType.BIKE, "DL01AB1234"
        );

        assertThrows(DuplicateAgentException.class,
            () -> deliveryService.registerAgent(101, request));
    }

    // ── Test: Get agent by ID ─────────────────────────────────────────
    @Test
    void getAgentById_existing_success() {
        when(deliveryRepository.findByAgentId(1)).thenReturn(Optional.of(verifiedAgent));

        AgentResponse response = deliveryService.getAgentById(1);

        assertEquals(1, response.getAgentId());
        assertEquals("Arjun Kumar", response.getFullName());
        assertEquals("VERIFIED", response.getStatus());
    }

    // ── Test: Get agent not found ─────────────────────────────────────
    @Test
    void getAgentById_notFound_throwsAgentNotFoundException() {
        when(deliveryRepository.findByAgentId(999)).thenReturn(Optional.empty());
        assertThrows(AgentNotFoundException.class, () -> deliveryService.getAgentById(999));
    }

    // ── Test: Update location ─────────────────────────────────────────
    @Test
    void updateLocation_validCoords_success() {
        when(deliveryRepository.findByAgentId(1)).thenReturn(Optional.of(verifiedAgent));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateLocationRequest request = new UpdateLocationRequest(28.6139, 77.2090);
        AgentResponse response = deliveryService.updateLocation(1, request);

        assertEquals(28.6139, response.getCurrentLatitude());
        assertEquals(77.2090, response.getCurrentLongitude());
        assertNotNull(response.getLocationUpdatedAt());
    }

    // ── Test: Go online when not verified → throws ────────────────────
    @Test
    void setAvailability_goOnlineWhenNotVerified_throws() {
        when(deliveryRepository.findByAgentId(2)).thenReturn(Optional.of(pendingAgent));

        SetAvailabilityRequest request = new SetAvailabilityRequest(true);
        assertThrows(AgentNotVerifiedException.class,
            () -> deliveryService.setAvailability(2, request));
    }

    // ── Test: Go online when verified → success ───────────────────────
    @Test
    void setAvailability_goOnlineWhenVerified_success() {
        verifiedAgent.setIsAvailable(false); // Start offline
        when(deliveryRepository.findByAgentId(1)).thenReturn(Optional.of(verifiedAgent));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SetAvailabilityRequest request = new SetAvailabilityRequest(true);
        AgentResponse response = deliveryService.setAvailability(1, request);

        assertTrue(response.getIsAvailable());
    }

    // ── Test: Go offline while delivering → throws ────────────────────
    @Test
    void setAvailability_goOfflineWhileDelivering_throwsAgentBusyException() {
        verifiedAgent.setCurrentOrderId(42); // Has active order
        when(deliveryRepository.findByAgentId(1)).thenReturn(Optional.of(verifiedAgent));

        SetAvailabilityRequest request = new SetAvailabilityRequest(false);
        assertThrows(AgentBusyException.class,
            () -> deliveryService.setAvailability(1, request));
    }

    // ── Test: Admin verifies agent ────────────────────────────────────
    @Test
    void verifyAgent_verifyAction_setsVerifiedStatus() {
        when(deliveryRepository.findByAgentId(2)).thenReturn(Optional.of(pendingAgent));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VerifyAgentRequest request = new VerifyAgentRequest("VERIFY", null);
        AgentResponse response = deliveryService.verifyAgent(2, request);

        assertEquals("VERIFIED", response.getStatus());
        assertTrue(response.getIsVerified());
    }

    @Test
    void verifyAgent_rejectAction_setsRejectedStatusAndRemarks() {
        when(deliveryRepository.findByAgentId(2)).thenReturn(Optional.of(pendingAgent));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VerifyAgentRequest request = new VerifyAgentRequest("REJECT", "Documents missing");
        AgentResponse response = deliveryService.verifyAgent(2, request);

        assertEquals("REJECTED", response.getStatus());
        assertFalse(response.getIsVerified());
        assertEquals("Documents missing", response.getAdminRemarks());
    }

    @Test
    void verifyAgent_suspendAction_setsSuspendedStatus() {
        when(deliveryRepository.findByAgentId(2)).thenReturn(Optional.of(pendingAgent));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VerifyAgentRequest request = new VerifyAgentRequest("SUSPEND", "Policy violation");
        AgentResponse response = deliveryService.verifyAgent(2, request);

        assertEquals("SUSPENDED", response.getStatus());
        assertFalse(response.getIsVerified());
        assertEquals("Policy violation", response.getAdminRemarks());
    }

    // ── Test: Assign order to eligible agent ──────────────────────────
    @Test
    void assignOrder_eligibleAgent_success() {
        when(deliveryRepository.findByAgentId(1)).thenReturn(Optional.of(verifiedAgent));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AssignOrderRequest request = new AssignOrderRequest(55, 28.6, 77.2);
        AgentResponse response = deliveryService.assignOrder(1, request);

        assertEquals(55, response.getCurrentOrderId());
    }

    // ── Test: Assign order to busy agent → throws ─────────────────────
    @Test
    void assignOrder_busyAgent_throwsAgentNotEligibleException() {
        verifiedAgent.setCurrentOrderId(10); // Already has order
        when(deliveryRepository.findByAgentId(1)).thenReturn(Optional.of(verifiedAgent));

        AssignOrderRequest request = new AssignOrderRequest(99, null, null);
        assertThrows(AgentNotEligibleException.class,
            () -> deliveryService.assignOrder(1, request));
    }

    // ── Test: Complete delivery ───────────────────────────────────────
    @Test
    void completeDelivery_validOrder_updatesStats() {
        verifiedAgent.setCurrentOrderId(55);
        int prevDeliveries = verifiedAgent.getTotalDeliveries();
        double prevEarnings = verifiedAgent.getTotalEarnings();

        when(deliveryRepository.findByAgentId(1)).thenReturn(Optional.of(verifiedAgent));
        when(deliveryHistoryRepository.existsByAgentIdAndOrderId(1, 55)).thenReturn(false);
        when(orderServiceClient.getOrderById(55)).thenReturn(ApiResponse.success(
                "Order fetched",
                OrderDetailsDTO.builder()
                        .orderId(55)
                        .customerId(9)
                        .customerName("Aman Verma")
                        .restaurantId(301)
                        .restaurantName("QuickBite Downtown")
                        .deliveryAddress("221B Baker Street")
                        .orderStatus("PICKED_UP")
                        .orderDate(LocalDateTime.now().minusMinutes(30))
                        .finalAmount(248.0)
                        .modeOfPayment("CARD")
                        .itemCount(2)
                        .build()
        ));
        when(restaurantServiceClient.getRestaurantById(301L)).thenReturn(ApiResponse.success(
                "Restaurant fetched",
                RestaurantDetailsDTO.builder()
                        .restaurantId(301L)
                        .name("QuickBite Downtown")
                        .address("12 Main Street")
                        .latitude(28.6139)
                        .longitude(77.2090)
                        .isOpen(true)
                        .approvalStatus("APPROVED")
                        .build()
        ));
        lenient().when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(deliveryHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AgentResponse response = deliveryService.completeDelivery(1, 55);

        assertNull(response.getCurrentOrderId());
        assertEquals(prevDeliveries + 1, response.getTotalDeliveries());
        assertEquals(prevEarnings + 50.0, response.getTotalEarnings(), 0.01);
        verify(orderServiceClient).updateOrderStatus(55, Map.of("status", "DELIVERED"));
        verify(deliveryHistoryRepository).save(any());
    }

    @Test
    void getAssignedOrder_success_returnsPickupAndDeliveryDetails() {
        verifiedAgent.setCurrentOrderId(55);
        when(deliveryRepository.findByAgentId(1)).thenReturn(Optional.of(verifiedAgent));
        when(orderServiceClient.getOrderById(55)).thenReturn(ApiResponse.success(
                "Order fetched",
                OrderDetailsDTO.builder()
                        .orderId(55)
                        .customerName("Aman Verma")
                        .restaurantId(301)
                        .restaurantName("QuickBite Downtown")
                        .deliveryAddress("221B Baker Street")
                        .orderStatus("ASSIGNED")
                        .orderDate(LocalDateTime.now().minusMinutes(20))
                        .estimatedDelivery(LocalDateTime.now().plusMinutes(20))
                        .build()
        ));
        when(restaurantServiceClient.getRestaurantById(301L)).thenReturn(ApiResponse.success(
                "Restaurant fetched",
                RestaurantDetailsDTO.builder()
                        .restaurantId(301L)
                        .name("QuickBite Downtown")
                        .address("12 Main Street")
                        .latitude(28.6139)
                        .longitude(77.2090)
                        .isOpen(true)
                        .approvalStatus("APPROVED")
                        .build()
        ));

        AssignedOrderResponse response = deliveryService.getAssignedOrder(1);

        assertEquals(55, response.getOrderId());
        assertEquals("12 Main Street", response.getPickupAddress());
        assertEquals("221B Baker Street", response.getDeliveryAddress());
    }

    @Test
    void pickUpOrder_validAssignment_updatesOrderStatus() {
        verifiedAgent.setCurrentOrderId(55);
        when(deliveryRepository.findByAgentId(1)).thenReturn(Optional.of(verifiedAgent));
        when(orderServiceClient.getOrderById(55)).thenReturn(ApiResponse.success(
                "Order fetched",
                OrderDetailsDTO.builder()
                        .orderId(55)
                        .orderStatus("READY_TO_PICK_UP")
                        .restaurantId(301)
                        .restaurantName("QuickBite Downtown")
                        .deliveryAddress("221B Baker Street")
                        .build()
        ));

        AgentResponse response = deliveryService.pickUpOrder(1, 55);

        assertEquals(55, response.getCurrentOrderId());
        verify(orderServiceClient).updateOrderStatus(55, Map.of("status", "PICKED_UP"));
    }

    // ── Test: Complete wrong order → throws ───────────────────────────
    @Test
    void completeDelivery_wrongOrder_throwsInvalidDeliveryException() {
        verifiedAgent.setCurrentOrderId(55);
        when(deliveryRepository.findByAgentId(1)).thenReturn(Optional.of(verifiedAgent));

        assertThrows(InvalidDeliveryException.class,
            () -> deliveryService.completeDelivery(1, 99)); // Wrong orderId
    }

    // ── Test: Update rating ───────────────────────────────────────────
    @Test
    void updateRating_validRating_updatesAvgRating() {
        when(deliveryRepository.findByAgentId(1)).thenReturn(Optional.of(verifiedAgent));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateRatingRequest request = new UpdateRatingRequest(4.8);
        AgentResponse response = deliveryService.updateRating(1, request);

        assertEquals(4.8, response.getAvgRating(), 0.01);
    }

    // ── Test: Get active deliveries ───────────────────────────────────
    @Test
    void getActiveDeliveries_returnsOnlyAgentsWithOrders() {
        verifiedAgent.setCurrentOrderId(55); // Has active order
        // pendingAgent.currentOrderId = null (no order)

        when(deliveryRepository.findAll()).thenReturn(List.of(verifiedAgent, pendingAgent));

        List<AgentResponse> active = deliveryService.getActiveDeliveries();

        assertEquals(1, active.size());
        assertEquals(55, active.get(0).getCurrentOrderId());
    }

    @Test
    void getAgentsByStatus_pending_returnsPendingAgents() {
        when(deliveryRepository.findByStatus(DeliveryAgent.AgentStatus.PENDING))
                .thenReturn(List.of(pendingAgent));

        List<AgentResponse> pending = deliveryService.getAgentsByStatus(DeliveryAgent.AgentStatus.PENDING);

        assertEquals(1, pending.size());
        assertEquals("PENDING", pending.get(0).getStatus());
    }
}
