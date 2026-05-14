package com.quickbite.order.serviceimpl;

import com.quickbite.order.cart.dto.CartItemResponse;
import com.quickbite.order.cart.dto.CartResponse;
import com.quickbite.order.cart.service.CartService;
import com.quickbite.order.dto.*;
import com.quickbite.order.entity.Order;
import com.quickbite.order.entity.Order.OrderStatus;
import com.quickbite.order.entity.Order.PaymentMode;
import com.quickbite.order.entity.OrderItem;
import com.quickbite.order.exception.InvalidStatusTransitionException;
import com.quickbite.order.feign.AuthServiceClient;
import com.quickbite.order.feign.DeliveryServiceClient;
import com.quickbite.order.feign.NotificationServiceClient;
import com.quickbite.order.feign.RestaurantServiceClient;
import com.quickbite.order.repository.OrderItemRepository;
import com.quickbite.order.repository.OrderRepository;
import com.quickbite.order.service.RabbitNotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplCoverageTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartService cartService;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private RestaurantServiceClient restaurantServiceClient;
    @Mock private NotificationServiceClient notificationServiceClient;
    @Mock private DeliveryServiceClient deliveryServiceClient;
    @Mock private RabbitNotificationPublisher rabbitNotificationPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    private CartResponse cartResponse;
    private RestaurantDTO restaurantDTO;
    private Order placedOrder;

    @BeforeEach
    void setUp() {
        CartItemResponse item = CartItemResponse.builder()
                .itemId(1)
                .menuItemId(101)
                .name("Paneer Butter Masala")
                .price(200.0)
                .quantity(1)
                .lineTotal(200.0)
                .customization("less spicy")
                .veg(true)
                .imageUrl("paneer.png")
                .build();

        cartResponse = CartResponse.builder()
                .cartId(10)
                .customerId(35)
                .restaurantId(1)
                .restaurantName("Spice Corner")
                .items(List.of(item))
                .itemCount(1)
                .subtotal(200.0)
                .discountAmount(20.0)
                .totalPrice(180.0)
                .promoCode("SAVE15")
                .empty(false)
                .build();

        restaurantDTO = new RestaurantDTO();
        restaurantDTO.setRestaurantId(1);
        restaurantDTO.setOwnerId(99);
        restaurantDTO.setName("Spice Corner");
        restaurantDTO.setCuisine("Indian");
        restaurantDTO.setOpen(true);
        restaurantDTO.setApproved(true);
        restaurantDTO.setApprovalStatus("APPROVED");
        restaurantDTO.setMinOrderAmount(0);
        restaurantDTO.setEstimatedDeliveryMin(20);
        restaurantDTO.setLatitude(null);
        restaurantDTO.setLongitude(null);

        placedOrder = new Order();
        placedOrder.setOrderId(500);
        placedOrder.setCustomerId(35);
        placedOrder.setCustomerName("Aman Verma");
        placedOrder.setRestaurantId(1);
        placedOrder.setRestaurantName("Spice Corner");
        placedOrder.setDeliveryAddress("Sector 18, Noida");
        placedOrder.setModeOfPayment(PaymentMode.COD);
        placedOrder.setOrderStatus(OrderStatus.PLACED);
        placedOrder.setTotalAmount(200.0);
        placedOrder.setDiscount(20.0);
        placedOrder.setFinalAmount(180.0);
        placedOrder.setOrderDate(LocalDateTime.of(2026, 5, 14, 10, 0));
        placedOrder.setEstimatedDelivery(LocalDateTime.of(2026, 5, 14, 10, 30));
        placedOrder.getOrderItems().add(buildOrderItem(1, 101, "Paneer Butter Masala", 200.0, 1));

        lenient().doNothing().when(cartService).archiveCartForOrder(anyInt(), anyInt());
        lenient().when(cartService.clearCart(anyInt())).thenReturn(cartResponse);
        lenient().when(rabbitNotificationPublisher.publish(any())).thenReturn(true);
        lenient().when(authServiceClient.getUserById(anyInt())).thenReturn(null);
    }

    @Test
    void placeOrder_shouldCreateOrderAndClearCart() {
        PlaceOrderRequest request = PlaceOrderRequest.builder()
                .modeOfPayment(PaymentMode.COD)
                .deliveryAddress("Sector 18, Noida")
                .specialInstructions("Call on arrival")
                .build();

        when(cartService.getCartByCustomer(35)).thenReturn(cartResponse);
        when(restaurantServiceClient.getRestaurantById(1)).thenReturn(ApiResponse.success("ok", restaurantDTO));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(500);
            return order;
        });

        OrderResponse response = orderService.placeOrder(35, "Aman Verma", request);

        assertEquals(500, response.getOrderId());
        assertEquals("Spice Corner", response.getRestaurantName());
        assertEquals(1, response.getItemCount());
        assertEquals(180.0, response.getFinalAmount(), 0.01);
        verify(cartService).archiveCartForOrder(35, 500);
        verify(cartService).clearCart(35);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void placeOrder_shouldRejectWhenMinimumNotMet() {
        restaurantDTO.setMinOrderAmount(500.0);

        PlaceOrderRequest request = PlaceOrderRequest.builder()
                .modeOfPayment(PaymentMode.COD)
                .deliveryAddress("Sector 18, Noida")
                .build();

        when(cartService.getCartByCustomer(35)).thenReturn(cartResponse);
        when(restaurantServiceClient.getRestaurantById(1)).thenReturn(ApiResponse.success("ok", restaurantDTO));

        assertThrows(com.quickbite.order.exception.RestaurantNotAvailableException.class,
                () -> orderService.placeOrder(35, "Aman Verma", request));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateStatus_shouldAdvanceOrderForOwner() {
        Order order = copyOrder(OrderStatus.PLACED);
        when(orderRepository.findById(500)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateStatus(500, OrderStatus.CONFIRMED, "OWNER");

        assertEquals(OrderStatus.CONFIRMED, response.getOrderStatus());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateStatus_shouldRejectInvalidTransition() {
        Order order = copyOrder(OrderStatus.PLACED);
        when(orderRepository.findById(500)).thenReturn(Optional.of(order));

        assertThrows(InvalidStatusTransitionException.class,
                () -> orderService.updateStatus(500, OrderStatus.DELIVERED, "AGENT"));
    }

    @Test
    void cancelOrder_shouldCancelCustomerOrderBeforePreparing() {
        Order order = copyOrder(OrderStatus.PLACED);
        when(orderRepository.findByOrderIdAndCustomerId(500, 35)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder(500, 35, false);

        assertEquals(OrderStatus.CANCELLED, response.getOrderStatus());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void getRevenueAnalytics_shouldAggregateRevenueAndPeakHours() {
        Order delivered = copyOrder(OrderStatus.DELIVERED);
        delivered.setOrderDate(LocalDateTime.of(2026, 5, 14, 9, 10));
        Order cancelled = copyOrder(OrderStatus.CANCELLED);
        cancelled.setOrderDate(LocalDateTime.of(2026, 5, 14, 9, 40));
        Order active = copyOrder(OrderStatus.PLACED);
        active.setOrderDate(LocalDateTime.of(2026, 5, 14, 11, 5));

        when(orderRepository.findByRestaurantIdAndOrderDateBetween(eq(1), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(delivered, cancelled, active));
        when(orderRepository.sumRevenueByRestaurantAndDateRange(eq(1), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(300.0);
        when(orderItemRepository.findTopSellingItemsByRestaurantAndDateRange(eq(1), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        new Object[]{101, "Paneer Butter Masala", 12L, 2400.5},
                        new Object[]{102, "Veg Biryani", 8L, 1600.0}
                ));
        when(restaurantServiceClient.getRestaurantById(1)).thenReturn(ApiResponse.success("ok", restaurantDTO));

        RevenueAnalyticsDTO analytics = orderService.getRevenueAnalytics(1);

        assertEquals(1, analytics.getRestaurantId());
        assertEquals("Spice Corner", analytics.getRestaurantName());
        assertEquals(300.0, analytics.getTotalRevenue(), 0.01);
        assertEquals(3L, analytics.getTotalOrders());
        assertEquals(1L, analytics.getDeliveredOrders());
        assertEquals(1L, analytics.getCancelledOrders());
        assertEquals(1L, analytics.getPendingOrders());
        assertThat(analytics.getTopSellingItems()).hasSize(2);
        assertEquals(9, analytics.getPeakHours().get(0).getHour());
        assertEquals("09:00-10:00", analytics.getPeakHours().get(0).getLabel());
    }

    private Order copyOrder(OrderStatus status) {
        Order order = new Order();
        order.setOrderId(500);
        order.setCustomerId(35);
        order.setCustomerName("Aman Verma");
        order.setRestaurantId(1);
        order.setRestaurantName("Spice Corner");
        order.setDeliveryAddress("Sector 18, Noida");
        order.setModeOfPayment(PaymentMode.COD);
        order.setOrderStatus(status);
        order.setTotalAmount(200.0);
        order.setDiscount(20.0);
        order.setFinalAmount(180.0);
        order.setOrderDate(LocalDateTime.of(2026, 5, 14, 10, 0));
        order.setEstimatedDelivery(LocalDateTime.of(2026, 5, 14, 10, 30));
        order.setDeliveryAgentId(status == OrderStatus.PLACED ? null : 77);
        order.getOrderItems().add(buildOrderItem(1, 101, "Paneer Butter Masala", 200.0, 1));
        return order;
    }

    private OrderItem buildOrderItem(int id, int menuItemId, String name, double price, int quantity) {
        OrderItem item = new OrderItem();
        item.setOrderItemId(id);
        item.setMenuItemId(menuItemId);
        item.setName(name);
        item.setPrice(price);
        item.setQuantity(quantity);
        item.setCustomization("less spicy");
        item.setVeg(true);
        item.setImageUrl("paneer.png");
        return item;
    }
}
