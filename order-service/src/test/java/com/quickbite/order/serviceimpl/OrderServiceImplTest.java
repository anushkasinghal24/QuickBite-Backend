package com.quickbite.order.serviceimpl;

import com.quickbite.order.dto.OrderResponse;
import com.quickbite.order.entity.Order;
import com.quickbite.order.entity.Order.OrderStatus;
import com.quickbite.order.entity.Order.PaymentMode;
import com.quickbite.order.entity.OrderItem;
import com.quickbite.order.feign.DeliveryServiceClient;
import com.quickbite.order.feign.NotificationServiceClient;
import com.quickbite.order.feign.RestaurantServiceClient;
import com.quickbite.order.repository.OrderItemRepository;
import com.quickbite.order.repository.OrderRepository;
import com.quickbite.order.cart.service.CartService;
import com.quickbite.order.service.RabbitNotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartService cartService;
    @Mock private RestaurantServiceClient restaurantServiceClient;
    @Mock private NotificationServiceClient notificationServiceClient;
    @Mock private DeliveryServiceClient deliveryServiceClient;
    @Mock private RabbitNotificationPublisher rabbitNotificationPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setOrderId(1);
        order.setCustomerId(35);
        order.setCustomerName("Aman Verma");
        order.setRestaurantId(1);
        order.setRestaurantName("Spice Corner");
        order.setDeliveryAddress("Sector 18, Noida");
        order.setOrderStatus(OrderStatus.PLACED);
        order.setModeOfPayment(PaymentMode.COD);
        order.setTotalAmount(500.0);
        order.setDiscount(50.0);
        order.setFinalAmount(450.0);
        order.setOrderDate(LocalDateTime.now());
        order.setEstimatedDelivery(LocalDateTime.now().plusMinutes(30));

        OrderItem item = new OrderItem();
        item.setOrderItemId(11);
        item.setMenuItemId(101);
        item.setName("Paneer Butter Masala");
        item.setPrice(250.0);
        item.setQuantity(2);
        item.setVeg(true);
        item.setOrder(order);
        order.getOrderItems().add(item);
    }

    @Test
    void getOrderById_shouldReturnMappedOrder() {
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(1);

        assertNotNull(response);
        assertEquals(1, response.getOrderId());
        assertEquals("Spice Corner", response.getRestaurantName());
        assertEquals(1, response.getOrderItems().size());
        assertEquals(450.0, response.getFinalAmount(), 0.01);
    }

    @Test
    void getOrderCount_shouldReturnRepositoryCount() {
        when(orderRepository.countByRestaurantId(1)).thenReturn(7L);

        long count = orderService.getOrderCount(1);

        assertEquals(7L, count);
    }
}
