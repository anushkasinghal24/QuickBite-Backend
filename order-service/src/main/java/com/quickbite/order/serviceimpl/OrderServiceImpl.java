package com.quickbite.order.serviceimpl;

import com.quickbite.order.dto.*;
import com.quickbite.order.entity.Order;
import com.quickbite.order.entity.Order.OrderStatus;
import com.quickbite.order.entity.Order.PaymentMode;
import com.quickbite.order.entity.OrderItem;
import com.quickbite.order.exception.*;
import com.quickbite.order.feign.*;
import com.quickbite.order.repository.OrderItemRepository;
import com.quickbite.order.repository.OrderRepository;
import com.quickbite.order.service.OrderService;
import com.quickbite.order.cart.dto.CartItemResponse;
import com.quickbite.order.cart.dto.CartResponse;
import com.quickbite.order.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OrderServiceImpl
 *
 * Full business logic for the order lifecycle as per PDF Section 4.5.
 *
 * ORDER PLACEMENT FLOW:
 *  1. Fetch cart from cart-service (validate non-empty)
 *  2. Validate restaurant is open + approved (restaurant-service)
 *  3. Validate cart total >= restaurant minimum order amount
 *  4. Create Order entity + snapshot CartItems â†’ OrderItems
 *  5. Save order (status = PLACED)
 *  6. Clear customer cart (cart-service)
 *  7. Send notifications â€” customer + restaurant owner (notification-service)
 *  8. Auto-assign delivery agent if available (delivery-service) [best-effort]
 *  9. Return OrderResponse
 *
 * STATUS TRANSITION RULES (enforced strictly):
 *  PLACED      â†’ CONFIRMED   (by Restaurant Owner)
 *  CONFIRMED   â†’ PREPARING   (by Restaurant Owner)
 *  PREPARING   â†’ PICKED_UP   (by Delivery Agent)
 *  PICKED_UP   â†’ DELIVERED   (by Delivery Agent)
 *  ANY         â†’ CANCELLED   (by Customer before PREPARING, or Admin anytime)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository           orderRepository;
    private final OrderItemRepository       orderItemRepository;
    private final CartService               cartService;
    private final RestaurantServiceClient   restaurantServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final DeliveryServiceClient     deliveryServiceClient;

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 1. PLACE ORDER
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    public OrderResponse placeOrder(int customerId, String customerName,
                                    PlaceOrderRequest request) {

        log.info("Placing order for customer {} | paymentMode={} | address={}",
                customerId, request.getModeOfPayment(), request.getDeliveryAddress());

        // â”€â”€ STEP 1: Fetch cart from cart-service â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        CartDTO cart = fetchCart(customerId);

        if (cart.isEmpty() || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new EmptyCartException(
                "Your cart is empty. Please add items before placing an order.");
        }

        int restaurantId = cart.getRestaurantId();

        // â”€â”€ STEP 2: Validate restaurant â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        RestaurantDTO restaurant = fetchRestaurant(restaurantId);

        if (!restaurant.isApproved()) {
            throw new RestaurantNotAvailableException(
                "Restaurant '" + restaurant.getName() + "' is not yet approved on the platform.");
        }
        if (!restaurant.isOpen()) {
            throw new RestaurantNotAvailableException(
                "Restaurant '" + restaurant.getName() + "' is currently closed.");
        }

        // â”€â”€ STEP 3: Validate minimum order amount â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        double cartTotal = cart.getTotalPrice();
        if (restaurant.getMinOrderAmount() > 0
                && cartTotal < restaurant.getMinOrderAmount()) {
            throw new RestaurantNotAvailableException(
                String.format("Minimum order amount for '%s' is â‚¹%.2f. " +
                              "Your cart total is â‚¹%.2f.",
                              restaurant.getName(),
                              restaurant.getMinOrderAmount(),
                              cartTotal));
        }

        // â”€â”€ STEP 4: Build Order entity â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setCustomerName(customerName);
        order.setRestaurantId(restaurantId);
        order.setRestaurantName(restaurant.getName());
        order.setModeOfPayment(request.getModeOfPayment());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setSpecialInstructions(request.getSpecialInstructions());
        order.setOrderStatus(OrderStatus.PLACED);
        order.setOrderDate(LocalDateTime.now());

        // Financial snapshot from cart
        double subtotal  = cart.getSubtotal();
        double discount  = cart.getDiscountAmount();
        double finalAmt  = cart.getTotalPrice();

        order.setTotalAmount(subtotal);
        order.setDiscount(discount);
        order.setFinalAmount(finalAmt);

        // Estimated delivery time (restaurant's estimate + current time)
        int etaMinutes = restaurant.getEstimatedDeliveryMin() > 0
                ? restaurant.getEstimatedDeliveryMin() : 30;
        order.setEstimatedDelivery(LocalDateTime.now().plusMinutes(etaMinutes));

        // â”€â”€ STEP 5: Snapshot CartItems â†’ OrderItems â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        for (CartItemDTO cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setMenuItemId(cartItem.getMenuItemId());
            orderItem.setName(cartItem.getName());
            orderItem.setPrice(cartItem.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setCustomization(cartItem.getCustomization());
            orderItem.setVeg(cartItem.isVeg());
            orderItem.setImageUrl(cartItem.getImageUrl());
            orderItem.setOrder(order);
            order.getOrderItems().add(orderItem);
        }

        // â”€â”€ STEP 6: Save order â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Order savedOrder = orderRepository.save(order);
        log.info("Order #{} placed successfully for customer {}", savedOrder.getOrderId(), customerId);

        // â”€â”€ STEP 7: Clear customer's cart â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        clearCartSilently(customerId, savedOrder.getOrderId());

        // â”€â”€ STEP 8: Send notifications â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // 8a. Notify customer â€” order placed
        sendNotificationSilently(NotificationRequest.builder()
                .recipientId(customerId)
                .type("ORDER")
                .title("Order Placed! ðŸŽ‰")
                .message(String.format("Your order #%d from %s has been placed successfully. " +
                                       "Total: â‚¹%.2f",
                        savedOrder.getOrderId(), restaurant.getName(), finalAmt))
                .relatedId(savedOrder.getOrderId())
                .deepLinkUrl("/orders/" + savedOrder.getOrderId() + "/track")
                .channel("APP")
                .audible(false)
                .build());

        // 8b. Notify restaurant owner â€” new incoming order
        sendNotificationSilently(NotificationRequest.builder()
                .recipientId(restaurant.getOwnerId())
                .type("NEW_ORDER_ALERT")
                .title("New Order #" + savedOrder.getOrderId() + " ðŸ””")
                .message(String.format("You have a new order from %s. Total: â‚¹%.2f",
                        customerName, finalAmt))
                .relatedId(savedOrder.getOrderId())
                .deepLinkUrl("/orders/restaurant/" + restaurantId)
                .channel("APP")
                .audible(true)
                .build());

        log.info("Notifications dispatched for order #{}", savedOrder.getOrderId());

        return mapToOrderResponse(savedOrder);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 2. GET ORDER BY ID
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(int orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                    "Order #" + orderId + " not found."));
        return mapToOrderResponse(order);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 3. GET ORDERS BY CUSTOMER (order history)
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryDTO> getOrdersByCustomer(int customerId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId)
                .stream()
                .map(this::mapToOrderSummary)
                .collect(Collectors.toList());
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 4. GET ACTIVE ORDERS BY CUSTOMER (tracking screen)
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getActiveOrdersByCustomer(int customerId) {
        return orderRepository.findActiveOrdersByCustomer(customerId)
                .stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 5. GET ORDERS BY RESTAURANT (restaurant dashboard)
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryDTO> getOrdersByRestaurant(int restaurantId) {
        return orderRepository.findByRestaurantIdOrderByOrderDateDesc(restaurantId)
                .stream()
                .map(this::mapToOrderSummary)
                .collect(Collectors.toList());
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 6. GET ORDERS BY AGENT
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryDTO> getOrdersByAgent(int agentId) {
        return orderRepository.findByDeliveryAgentIdOrderByOrderDateDesc(agentId)
                .stream()
                .map(this::mapToOrderSummary)
                .collect(Collectors.toList());
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 7. GET ALL ORDERS (Admin)
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryDTO> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::mapToOrderSummary)
                .collect(Collectors.toList());
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 8. GET ALL ACTIVE ORDERS (Admin)
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryDTO> getAllActiveOrders() {
        return orderRepository.findAllActiveOrders()
                .stream()
                .map(this::mapToOrderSummary)
                .collect(Collectors.toList());
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 9. UPDATE ORDER STATUS
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    public OrderResponse updateStatus(int orderId, OrderStatus newStatus, String callerRole) {
        log.info("Updating order #{} to status {} by role {}", orderId, newStatus, callerRole);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order #" + orderId + " not found."));

        OrderStatus current = order.getOrderStatus();

        // â”€â”€ Validate transition â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        validateStatusTransition(current, newStatus, callerRole);

        order.setOrderStatus(newStatus);
        Order saved = orderRepository.save(order);

        // â”€â”€ Notify customer of status change â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        String notifTitle   = getStatusNotificationTitle(newStatus);
        String notifMessage = getStatusNotificationMessage(newStatus, saved);

        sendNotificationSilently(NotificationRequest.builder()
                .recipientId(saved.getCustomerId())
                .type("ORDER")
                .title(notifTitle)
                .message(notifMessage)
                .relatedId(orderId)
                .deepLinkUrl("/orders/" + orderId + "/track")
                .channel("APP")
                .build());

        // â”€â”€ On DELIVERED: notify agent completion â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (newStatus == OrderStatus.DELIVERED && saved.getDeliveryAgentId() != null) {
            completeDeliverySilently(saved.getDeliveryAgentId(), orderId);
            log.info("Delivery complete. Agent {} freed for next order.", saved.getDeliveryAgentId());
        }

        log.info("Order #{} status updated: {} â†’ {}", orderId, current, newStatus);
        return mapToOrderResponse(saved);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 10. ASSIGN DELIVERY AGENT
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    public OrderResponse assignDeliveryAgent(int orderId, int agentId) {
        log.info("Assigning agent {} to order #{}", agentId, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order #" + orderId + " not found."));

        if (order.isTerminal()) {
            throw new InvalidStatusTransitionException(
                "Cannot assign agent to a " + order.getOrderStatus() + " order.");
        }

        order.setDeliveryAgentId(agentId);
        Order saved = orderRepository.save(order);

        // Tell delivery-service to mark this agent as busy
        try {
            deliveryServiceClient.assignOrderToAgent(agentId, orderId);
        } catch (Exception e) {
            log.warn("Could not update delivery-service for agent assignment: {}", e.getMessage());
        }

        // Notify customer that an agent has been assigned
        sendNotificationSilently(NotificationRequest.builder()
                .recipientId(saved.getCustomerId())
                .type("DELIVERY")
                .title("Delivery Agent Assigned ðŸ›µ")
                .message("A delivery agent has been assigned to your order #" + orderId + ".")
                .relatedId(orderId)
                .deepLinkUrl("/orders/" + orderId + "/track")
                .channel("APP")
                .build());

        return mapToOrderResponse(saved);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 11. CANCEL ORDER
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    public OrderResponse cancelOrder(int orderId, int customerId, boolean isAdmin) {
        log.info("Cancel request for order #{} by customer {} (admin={})",
                orderId, customerId, isAdmin);

        Order order;

        if (isAdmin) {
            // Admin can cancel any order by ID
            order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(
                        "Order #" + orderId + " not found."));
        } else {
            // Customer can only cancel their own order
            order = orderRepository.findByOrderIdAndCustomerId(orderId, customerId)
                    .orElseThrow(() -> new OrderNotFoundException(
                        "Order #" + orderId + " not found for your account."));
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new OrderCancellationException("Order #" + orderId + " is already cancelled.");
        }

        if (order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new OrderCancellationException(
                "Order #" + orderId + " has already been delivered and cannot be cancelled.");
        }

        // Customer: can only cancel before PREPARING (PDF Section 2.2)
        if (!isAdmin && !order.isCancellable()) {
            throw new OrderCancellationException(
                "Order #" + orderId + " cannot be cancelled. " +
                "Cancellation is only allowed before the restaurant starts preparing your food. " +
                "Current status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        // â”€â”€ Free delivery agent if one was assigned â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (saved.getDeliveryAgentId() != null) {
            completeDeliverySilently(saved.getDeliveryAgentId(), orderId);
        }

        // â”€â”€ Notify customer of cancellation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        sendNotificationSilently(NotificationRequest.builder()
                .recipientId(saved.getCustomerId())
                .type("ORDER")
                .title("Order Cancelled")
                .message(String.format("Order #%d has been cancelled. " +
                                       "If you paid online, a refund will be processed in 3-5 business days.",
                        orderId))
                .relatedId(orderId)
                .deepLinkUrl("/orders/" + orderId)
                .channel("APP")
                .build());

        // â”€â”€ Notify restaurant â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        sendNotificationSilently(NotificationRequest.builder()
                .recipientId(saved.getRestaurantId())
                .type("ORDER")
                .title("Order #" + orderId + " Cancelled")
                .message("Order #" + orderId + " from " + saved.getCustomerName() +
                         " has been cancelled.")
                .relatedId(orderId)
                .deepLinkUrl("/restaurant/orders/" + orderId)
                .channel("APP")
                .build());

        log.info("Order #{} cancelled successfully.", orderId);
        return mapToOrderResponse(saved);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 12. REORDER FROM HISTORY
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    public OrderResponse reorderFromHistory(int originalOrderId, int customerId,
                                            PlaceOrderRequest request) {
        log.info("Reorder: customer {} reordering from order #{}", customerId, originalOrderId);

        // Fetch the original order
        Order originalOrder = orderRepository.findByOrderIdAndCustomerId(
                originalOrderId, customerId)
                .orElseThrow(() -> new OrderNotFoundException(
                    "Order #" + originalOrderId + " not found in your order history."));

        // Validate restaurant is still open/approved
        RestaurantDTO restaurant = fetchRestaurant(originalOrder.getRestaurantId());

        if (!restaurant.isApproved() || !restaurant.isOpen()) {
            throw new RestaurantNotAvailableException(
                "Restaurant '" + restaurant.getName() +
                "' is not currently available for reorder.");
        }

        // Build a new order from the original's items
        Order newOrder = new Order();
        newOrder.setCustomerId(customerId);
        newOrder.setCustomerName(originalOrder.getCustomerName());
        newOrder.setRestaurantId(originalOrder.getRestaurantId());
        newOrder.setRestaurantName(originalOrder.getRestaurantName());
        newOrder.setModeOfPayment(request.getModeOfPayment());
        newOrder.setDeliveryAddress(request.getDeliveryAddress());
        newOrder.setSpecialInstructions(request.getSpecialInstructions());
        newOrder.setOrderStatus(OrderStatus.PLACED);
        newOrder.setOrderDate(LocalDateTime.now());

        // Snapshot items from the original order (prices may differ â€” use original snapshot)
        double total = 0;
        for (OrderItem originalItem : originalOrder.getOrderItems()) {
            OrderItem newItem = new OrderItem();
            newItem.setMenuItemId(originalItem.getMenuItemId());
            newItem.setName(originalItem.getName());
            newItem.setPrice(originalItem.getPrice());
            newItem.setQuantity(originalItem.getQuantity());
            newItem.setCustomization(originalItem.getCustomization());
            newItem.setVeg(originalItem.isVeg());
            newItem.setImageUrl(originalItem.getImageUrl());
            newItem.setOrder(newOrder);
            newOrder.getOrderItems().add(newItem);
            total += newItem.getLineTotal();
        }

        newOrder.setTotalAmount(total);
        newOrder.setDiscount(0);  // No promo code on reorder (customer can apply new one)
        newOrder.setFinalAmount(total);

        int etaMinutes = restaurant.getEstimatedDeliveryMin() > 0
                ? restaurant.getEstimatedDeliveryMin() : 30;
        newOrder.setEstimatedDelivery(LocalDateTime.now().plusMinutes(etaMinutes));

        Order saved = orderRepository.save(newOrder);

        // Notify customer + restaurant
        sendNotificationSilently(NotificationRequest.builder()
                .recipientId(customerId)
                .type("ORDER")
                .title("Reorder Placed! ðŸŽ‰")
                .message(String.format("Your reorder #%d from %s has been placed. Total: â‚¹%.2f",
                        saved.getOrderId(), restaurant.getName(), total))
                .relatedId(saved.getOrderId())
                .deepLinkUrl("/orders/" + saved.getOrderId() + "/track")
                .channel("APP")
                .audible(false)
                .build());

        sendNotificationSilently(NotificationRequest.builder()
                .recipientId(restaurant.getOwnerId())
                .type("NEW_ORDER_ALERT")
                .title("New Reorder #" + saved.getOrderId() + " ðŸ””")
                .message("New reorder from " + originalOrder.getCustomerName() +
                         ". Total: â‚¹" + String.format("%.2f", total))
                .relatedId(saved.getOrderId())
                .deepLinkUrl("/orders/restaurant/" + originalOrder.getRestaurantId())
                .channel("APP")
                .audible(true)
                .build());

        log.info("Reorder #{} placed (original was #{})", saved.getOrderId(), originalOrderId);
        return mapToOrderResponse(saved);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 13. REVENUE ANALYTICS (restaurant owner)
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    @Transactional(readOnly = true)
    public RevenueAnalyticsDTO getRevenueAnalytics(int restaurantId) {
        LocalDateTime now   = LocalDateTime.now();
        LocalDateTime today = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart  = now.minusDays(7);
        LocalDateTime monthStart = now.minusDays(30);

        List<Order> monthlyOrders = orderRepository.findByRestaurantIdAndOrderDateBetween(
                restaurantId, monthStart, now);

        double totalRevenue   = orderRepository.sumRevenueByRestaurantAndDateRange(
                restaurantId, LocalDateTime.of(2000, 1, 1, 0, 0), now);
        double dailyRevenue   = orderRepository.sumRevenueByRestaurantAndDateRange(
                restaurantId, today, now);
        double weeklyRevenue  = orderRepository.sumRevenueByRestaurantAndDateRange(
                restaurantId, weekStart, now);
        double monthlyRevenue = orderRepository.sumRevenueByRestaurantAndDateRange(
                restaurantId, monthStart, now);

        long totalOrders = monthlyOrders.size();
        long deliveredOrders = monthlyOrders.stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.DELIVERED)
                .count();
        long cancelledOrders = monthlyOrders.stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.CANCELLED)
                .count();
        long pendingOrders = totalOrders - deliveredOrders - cancelledOrders;

        List<TopSellingItemDTO> topSellingItems = orderItemRepository
                .findTopSellingItemsByRestaurantAndDateRange(restaurantId, monthStart, now)
                .stream()
                .limit(5)
                .map(this::mapToTopSellingItem)
                .collect(Collectors.toList());

        List<PeakHourDTO> peakHours = buildPeakHours(monthlyOrders);

        return RevenueAnalyticsDTO.builder()
                .restaurantId(restaurantId)
                .restaurantName(fetchRestaurant(restaurantId).getName())
                .totalRevenue(totalRevenue)
                .dailyRevenue(dailyRevenue)
                .weeklyRevenue(weeklyRevenue)
                .monthlyRevenue(monthlyRevenue)
                .totalOrders(totalOrders)
                .deliveredOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .pendingOrders(Math.max(0, pendingOrders))
                .topSellingItems(topSellingItems)
                .peakHours(peakHours)
                .build();
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // 14. GET ORDER COUNT
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    @Override
    @Transactional(readOnly = true)
    public long getOrderCount(int restaurantId) {
        return orderRepository.countByRestaurantId(restaurantId);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    private TopSellingItemDTO mapToTopSellingItem(Object[] row) {
        int menuItemId = ((Number) row[0]).intValue();
        String itemName = (String) row[1];
        long quantitySold = ((Number) row[2]).longValue();
        double revenue = row.length > 3 && row[3] != null
                ? ((Number) row[3]).doubleValue()
                : 0.0;

        return TopSellingItemDTO.builder()
                .menuItemId(menuItemId)
                .itemName(itemName)
                .quantitySold(quantitySold)
                .revenue(Math.round(revenue * 100.0) / 100.0)
                .build();
    }

    private List<PeakHourDTO> buildPeakHours(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.getOrderDate() != null)
                .filter(order -> order.getOrderStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.groupingBy(
                        order -> order.getOrderDate().getHour(),
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(5)
                .map(entry -> PeakHourDTO.builder()
                        .hour(entry.getKey())
                        .label(formatHourLabel(entry.getKey()))
                        .orderCount(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private String formatHourLabel(int hour) {
        int nextHour = (hour + 1) % 24;
        return String.format("%02d:00-%02d:00", hour, nextHour);
    }

    // PRIVATE HELPERS
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    /**
     * Fetch cart from cart-service via Feign.
     * Throws ServiceUnavailableException if service is down.
     */
    private CartDTO fetchCart(int customerId) {
        try {
            CartResponse response = cartService.getCartByCustomer(customerId);
            if (response != null) {
                return mapToCartDTO(response);
            }
        } catch (Exception e) {
            log.error("Failed to fetch cart for customer {}: {}", customerId, e.getMessage());
        }
        throw new ServiceUnavailableException(
            "Unable to fetch your cart. Please try again.");
    }

    /**
     * Fetch restaurant from restaurant-service via Feign.
     * Falls back to a permissive stub so order-service can work
     * standalone during development.
     */
    private RestaurantDTO fetchRestaurant(int restaurantId) {
        try {
            ApiResponse<RestaurantDTO> response =
                    restaurantServiceClient.getRestaurantById(restaurantId);
            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("restaurant-service unavailable, using stub for restaurant {}: {}",
                    restaurantId, e.getMessage());
        }

        // â”€â”€ STUB (remove once restaurant-service is built) â”€â”€â”€â”€â”€â”€â”€â”€â”€
        log.warn("Using STUB restaurant data for restaurant {}. " +
                 "Remove once restaurant-service is running.", restaurantId);
        RestaurantDTO stub = new RestaurantDTO();
        stub.setRestaurantId(restaurantId);
        stub.setOwnerId(restaurantId);
        stub.setName("Restaurant #" + restaurantId);
        stub.setOpen(true);
        stub.setApproved(true);
        stub.setMinOrderAmount(0);
        stub.setEstimatedDeliveryMin(30);
        return stub;
    }

    /**
     * Clear cart silently â€” failure must not break order placement.
     */
    private void clearCartSilently(int customerId, int orderId) {
        try {
            cartService.clearCart(customerId);
            log.info("Cart cleared for customer {} after order #{}", customerId, orderId);
        } catch (Exception e) {
            log.warn("Could not clear cart for customer {} after order #{}: {}",
                    customerId, orderId, e.getMessage());
            // Non-fatal â€” cart will auto-expire or customer can clear manually
        }
    }

    /**
     * Send notification silently â€” failure must never break the order flow.
     * As per PDF Section 6: "graceful degradation on notification service outage"
     */
    private void sendNotificationSilently(NotificationRequest request) {
        try {
            notificationServiceClient.sendNotification(request);
        } catch (Exception e) {
            log.warn("Notification dispatch failed (non-fatal) for recipient {}: {}",
                    request.getRecipientId(), e.getMessage());
        }
    }

    /**
     * Tell delivery-service order is complete â€” failure is non-fatal.
     */
 private CartDTO mapToCartDTO(CartResponse response) {
        CartDTO cart = new CartDTO();
        cart.setCartId(response.getCartId());
        cart.setCustomerId(response.getCustomerId());
        cart.setRestaurantId(response.getRestaurantId());
        cart.setRestaurantName(response.getRestaurantName());
        cart.setItems(response.getItems() == null ? List.of() : response.getItems().stream()
                .map(this::mapToCartItemDTO)
                .collect(Collectors.toList()));
        cart.setItemCount(response.getItemCount());
        cart.setSubtotal(response.getSubtotal());
        cart.setDiscountAmount(response.getDiscountAmount());
        cart.setTotalPrice(response.getTotalPrice());
        cart.setPromoCode(response.getPromoCode());
        cart.setEmpty(response.isEmpty());
        return cart;
    }

    private CartItemDTO mapToCartItemDTO(CartItemResponse item) {
        CartItemDTO dto = new CartItemDTO();
        dto.setItemId(item.getItemId());
        dto.setMenuItemId(item.getMenuItemId());
        dto.setName(item.getName());
        dto.setPrice(item.getPrice());
        dto.setQuantity(item.getQuantity());
        dto.setLineTotal(item.getLineTotal());
        dto.setCustomization(item.getCustomization());
        dto.setVeg(item.isVeg());
        dto.setImageUrl(item.getImageUrl());
        return dto;
    }

 private void completeDeliverySilently(int agentId, int orderId) {
        try {
            deliveryServiceClient.completeDelivery(agentId, orderId);
        } catch (Exception e) {
            log.warn("Could not notify delivery-service of completion for agent {}: {}",
                    agentId, e.getMessage());
        }
    }

    /**
     * Validate that the requested status transition is legal.
     *
     * Allowed transitions:
     *  PLACED    â†’ CONFIRMED  (OWNER or ADMIN)
     *  CONFIRMED â†’ PREPARING  (OWNER or ADMIN)
     *  PREPARING â†’ PICKED_UP  (AGENT or ADMIN)
     *  PICKED_UP â†’ DELIVERED  (AGENT or ADMIN)
     *  ANY       â†’ CANCELLED  (handled by cancelOrder, not here)
     */
    private void validateStatusTransition(OrderStatus current,
                                          OrderStatus requested,
                                          String callerRole) {

        if (current == OrderStatus.CANCELLED) {
            throw new InvalidStatusTransitionException(
                "Cannot change status of a cancelled order.");
        }
        if (current == OrderStatus.DELIVERED) {
            throw new InvalidStatusTransitionException(
                "Cannot change status of a delivered order.");
        }
        if (requested == OrderStatus.CANCELLED) {
            throw new InvalidStatusTransitionException(
                "Use the cancel endpoint to cancel an order.");
        }

        boolean isAdmin = "ADMIN".equals(callerRole);
        if (isAdmin) return;  // Admin can make any non-cancelled transition

        boolean isOwner = "OWNER".equals(callerRole);
        boolean isAgent = "AGENT".equals(callerRole);

        switch (requested) {
            case CONFIRMED:
                if (!isOwner)
                    throw new InvalidStatusTransitionException(
                        "Only a Restaurant Owner can confirm an order.");
                if (current != OrderStatus.PLACED)
                    throw new InvalidStatusTransitionException(
                        "Order must be PLACED to be CONFIRMED. Current: " + current);
                break;

            case PREPARING:
                if (!isOwner)
                    throw new InvalidStatusTransitionException(
                        "Only a Restaurant Owner can mark an order as PREPARING.");
                if (current != OrderStatus.CONFIRMED)
                    throw new InvalidStatusTransitionException(
                        "Order must be CONFIRMED before PREPARING. Current: " + current);
                break;

            case PICKED_UP:
                if (!isAgent)
                    throw new InvalidStatusTransitionException(
                        "Only a Delivery Agent can mark an order as PICKED_UP.");
                if (current != OrderStatus.PREPARING)
                    throw new InvalidStatusTransitionException(
                        "Order must be PREPARING before PICKED_UP. Current: " + current);
                break;

            case DELIVERED:
                if (!isAgent)
                    throw new InvalidStatusTransitionException(
                        "Only a Delivery Agent can mark an order as DELIVERED.");
                if (current != OrderStatus.PICKED_UP)
                    throw new InvalidStatusTransitionException(
                        "Order must be PICKED_UP before DELIVERED. Current: " + current);
                break;

            default:
                throw new InvalidStatusTransitionException(
                    "Invalid target status: " + requested);
        }
    }

    // â”€â”€ Notification message helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private String getStatusNotificationTitle(OrderStatus status) {
        return switch (status) {
            case CONFIRMED  -> "Order Confirmed âœ…";
            case PREPARING  -> "Your Food is Being Prepared ðŸ‘¨â€ðŸ³";
            case PICKED_UP  -> "Order Picked Up ðŸ›µ";
            case DELIVERED  -> "Order Delivered! ðŸŽ‰";
            default         -> "Order Update";
        };
    }

    private String getStatusNotificationMessage(OrderStatus status, Order order) {
        return switch (status) {
            case CONFIRMED -> String.format(
                "Order #%d confirmed by %s! We'll start preparing your food soon.",
                order.getOrderId(), order.getRestaurantName());
            case PREPARING -> String.format(
                "Order #%d is now being prepared by %s. Hang tight!",
                order.getOrderId(), order.getRestaurantName());
            case PICKED_UP -> String.format(
                "Order #%d has been picked up and is on the way to you!",
                order.getOrderId());
            case DELIVERED -> String.format(
                "Order #%d has been delivered. Enjoy your meal! " +
                "Don't forget to rate your experience.",
                order.getOrderId());
            default -> "Your order #" + order.getOrderId() + " has been updated.";
        };
    }

    // â”€â”€ Mapping helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .customerName(order.getCustomerName())
                .restaurantId(order.getRestaurantId())
                .restaurantName(order.getRestaurantName())
                .deliveryAgentId(order.getDeliveryAgentId())
                .totalAmount(order.getTotalAmount())
                .discount(order.getDiscount())
                .finalAmount(order.getFinalAmount())
                .modeOfPayment(order.getModeOfPayment())
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getOrderDate())
                .estimatedDelivery(order.getEstimatedDelivery())
                .deliveryAddress(order.getDeliveryAddress())
                .specialInstructions(order.getSpecialInstructions())
                .orderItems(itemResponses)
                .itemCount(itemResponses.size())
                .cancellable(order.isCancellable())
                .build();
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .orderItemId(item.getOrderItemId())
                .menuItemId(item.getMenuItemId())
                .name(item.getName())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .lineTotal(Math.round(item.getLineTotal() * 100.0) / 100.0)
                .customization(item.getCustomization())
                .veg(item.isVeg())
                .imageUrl(item.getImageUrl())
                .build();
    }

    private OrderSummaryDTO mapToOrderSummary(Order order) {
        return OrderSummaryDTO.builder()
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .customerName(order.getCustomerName())
                .restaurantId(order.getRestaurantId())
                .restaurantName(order.getRestaurantName())
                .orderStatus(order.getOrderStatus())
                .finalAmount(order.getFinalAmount())
                .modeOfPayment(order.getModeOfPayment())
                .orderDate(order.getOrderDate())
                .itemCount(order.getOrderItems().size())
                .cancellable(order.isCancellable())
                .build();
    }
}
