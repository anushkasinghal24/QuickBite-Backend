package com.quickbite.delivery_service.serviceimpl;

import com.quickbite.delivery_service.dto.RegisterAgentRequest;
import com.quickbite.delivery_service.dto.SetAvailabilityRequest;
import com.quickbite.delivery_service.entity.DeliveryAgent;
import com.quickbite.delivery_service.exception.AgentNotVerifiedException;
import com.quickbite.delivery_service.exception.DuplicateAgentException;
import com.quickbite.delivery_service.feign.NotificationServiceClient;
import com.quickbite.delivery_service.feign.OrderServiceClient;
import com.quickbite.delivery_service.feign.RestaurantServiceClient;
import com.quickbite.delivery_service.repository.DeliveryRepository;
import com.quickbite.delivery_service.service.RabbitNotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock private DeliveryRepository deliveryRepository;
    @Mock private NotificationServiceClient notificationServiceClient;
    @Mock private OrderServiceClient orderServiceClient;
    @Mock private RestaurantServiceClient restaurantServiceClient;
    @Mock private RabbitNotificationPublisher rabbitNotificationPublisher;

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    private DeliveryAgent pendingAgent;

    @BeforeEach
    void setUp() {
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
                .build();
    }

    @Test
    void registerAgent_shouldCreateAgentWhenUserDoesNotExist() {
        when(deliveryRepository.existsByUserId(103)).thenReturn(false);
        when(deliveryRepository.save(any(DeliveryAgent.class))).thenReturn(pendingAgent);
        when(rabbitNotificationPublisher.publish(any())).thenReturn(false);

        RegisterAgentRequest request = new RegisterAgentRequest(
                "New Agent", "9000000001",
                DeliveryAgent.VehicleType.BIKE, "DL03EF9999"
        );

        var response = deliveryService.registerAgent(103, request);

        assertEquals("PENDING", response.getStatus());
        verify(deliveryRepository).save(any(DeliveryAgent.class));
    }

    @Test
    void registerAgent_shouldRejectDuplicateUser() {
        when(deliveryRepository.existsByUserId(101)).thenReturn(true);

        RegisterAgentRequest request = new RegisterAgentRequest(
                "Arjun Kumar", "9876543210",
                DeliveryAgent.VehicleType.BIKE, "DL01AB1234"
        );

        assertThrows(DuplicateAgentException.class,
                () -> deliveryService.registerAgent(101, request));

        verify(deliveryRepository, never()).save(any(DeliveryAgent.class));
    }

    @Test
    void setAvailability_shouldRejectGoingOnlineWhenNotVerified() {
        when(deliveryRepository.findByAgentId(2)).thenReturn(java.util.Optional.of(pendingAgent));

        SetAvailabilityRequest request = new SetAvailabilityRequest(true);

        assertThrows(AgentNotVerifiedException.class,
                () -> deliveryService.setAvailability(2, request));
    }
}
