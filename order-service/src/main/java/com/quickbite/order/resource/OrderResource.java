package com.quickbite.order.resource;

import com.quickbite.order.config.JwtUtil;
import com.quickbite.order.dto.*;
import com.quickbite.order.entity.Order.OrderStatus;
import com.quickbite.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OrderResource â€” REST API Controller
 *
 * As per PDF Section 4.5:
 *  POST   /orders                              â†’ placeOrder (CUSTOMER)
 *  GET    /orders/{orderId}                    â†’ getOrderById (any auth)
 *  GET    /orders/customer/{customerId}        â†’ getOrdersByCustomer
 *  GET    /orders/customer/{customerId}/active â†’ getActiveOrders (tracking)
 *  GET    /orders/restaurant/{restaurantId}    â†’ getOrdersByRestaurant (OWNER/ADMIN)
 *  GET    /orders/agent/{agentId}              â†’ getOrdersByAgent (AGENT/ADMIN)
 *  GET    /orders/all                          â†’ getAllOrders (ADMIN)
 *  GET    /orders/all/active                   â†’ getAllActiveOrders (ADMIN)
 *  PUT    /orders/{orderId}/status             â†’ updateStatus (OWNER/AGENT only)
 *  PUT    /orders/{orderId}/accept             â†’ accept order (OWNER only)
 *  PUT    /orders/{orderId}/agent              â†’ assignDeliveryAgent (ADMIN)
 *  PUT    /orders/{orderId}/cancel             â†’ cancelOrder (CUSTOMER/ADMIN)
 *  POST   /orders/{orderId}/reorder            â†’ reorderFromHistory (CUSTOMER)
 *  GET    /orders/restaurant/{id}/analytics    â†’ getRevenueAnalytics (OWNER/ADMIN)
 *  GET    /orders/count/{restaurantId}         â†’ getOrderCount (ADMIN)
 *
 * JWT is mandatory for all endpoints (configured in SecurityConfig).
 * userId and role are extracted from the token via request attributes
 * set by JwtAuthenticationFilter.
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderResource {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // POST /orders
    // Place a new order from the customer's cart
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            HttpServletRequest httpRequest) {

        int    customerId    = extractUserId(httpRequest);
        String customerName  = extractFullName(httpRequest);

        log.info("POST /orders â€” customerId={} | mode={}", customerId, request.getModeOfPayment());

        OrderResponse order = orderService.placeOrder(customerId, customerName, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully!", order));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // GET /orders/{orderId}
    // Get a single order by ID (any authenticated user)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable int orderId) {

        log.info("GET /orders/{}", orderId);
        OrderResponse order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.success("Order fetched successfully", order));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // GET /orders/customer/{customerId}
    // Full order history for a customer
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderSummaryDTO>>> getOrdersByCustomer(
            @PathVariable int customerId) {

        log.info("GET /orders/customer/{}", customerId);
        List<OrderSummaryDTO> orders = orderService.getOrdersByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(
                "Order history fetched (" + orders.size() + " orders)", orders));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // GET /orders/customer/{customerId}/active
    // In-flight orders for the customer's live tracking screen
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @GetMapping("/customer/{customerId}/active")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getActiveOrdersByCustomer(
            @PathVariable int customerId) {

        log.info("GET /orders/customer/{}/active", customerId);
        List<OrderResponse> orders = orderService.getActiveOrdersByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(
                "Active orders fetched (" + orders.size() + ")", orders));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // GET /orders/restaurant/{restaurantId}
    // All orders for a restaurant dashboard
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByRestaurant(
            @PathVariable int restaurantId) {

        log.info("GET /orders/restaurant/{}", restaurantId);
        List<OrderResponse> orders = orderService.getOrdersByRestaurant(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(
                "Restaurant orders fetched (" + orders.size() + ")", orders));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // GET /orders/agent/{agentId}
    // All orders assigned to a delivery agent
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @GetMapping("/agent/{agentId}")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderSummaryDTO>>> getOrdersByAgent(
            @PathVariable int agentId) {

        log.info("GET /orders/agent/{}", agentId);
        List<OrderSummaryDTO> orders = orderService.getOrdersByAgent(agentId);
        return ResponseEntity.ok(ApiResponse.success(
                "Agent orders fetched (" + orders.size() + ")", orders));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // GET /orders/all
    // All orders platform-wide (Admin only)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderSummaryDTO>>> getAllOrders() {
        log.info("GET /orders/all â€” Admin request");
        List<OrderSummaryDTO> orders = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponse.success(
                "All orders fetched (" + orders.size() + ")", orders));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // GET /orders/all/active
    // All active (non-terminal) orders â€” Admin monitoring
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @GetMapping("/all/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderSummaryDTO>>> getAllActiveOrders() {
        log.info("GET /orders/all/active â€” Admin request");
        List<OrderSummaryDTO> orders = orderService.getAllActiveOrders();
        return ResponseEntity.ok(ApiResponse.success(
                "Active orders fetched (" + orders.size() + ")", orders));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // PUT /orders/{orderId}/status
    // Update order status (Restaurant Owner / Delivery Agent / Admin)
    // Body: { "status": "CONFIRMED" }
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('OWNER','AGENT')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable int orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            HttpServletRequest httpRequest) {

        String role = extractRole(httpRequest);
        log.info("PUT /orders/{}/status â€” newStatus={} by role={}", orderId, request.getStatus(), role);

        OrderResponse order = orderService.updateStatus(orderId, request.getStatus(), role);
        return ResponseEntity.ok(ApiResponse.success(
                "Order #" + orderId + " status updated to " + request.getStatus(), order));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // PUT /orders/{orderId}/accept
    // Quick action for restaurant owners to confirm an incoming order
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @PutMapping("/{orderId}/accept")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<OrderResponse>> acceptOrder(
            @PathVariable int orderId,
            HttpServletRequest httpRequest) {

        String role = extractRole(httpRequest);
        log.info("PUT /orders/{}/accept by role={}", orderId, role);

        OrderResponse order = orderService.updateStatus(orderId, OrderStatus.CONFIRMED, role);
        return ResponseEntity.ok(ApiResponse.success(
                "Order #" + orderId + " accepted successfully", order));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // PUT /orders/{orderId}/agent
    // Assign a delivery agent to an order (Admin only)
    // Body: { "agentId": 5 }
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @PutMapping("/{orderId}/agent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> assignDeliveryAgent(
            @PathVariable int orderId,
            @Valid @RequestBody AssignDeliveryAgentRequest request) {

        log.info("PUT /orders/{}/agent â€” agentId={}", orderId, request.getAgentId());
        OrderResponse order = orderService.assignDeliveryAgent(orderId, request.getAgentId());
        return ResponseEntity.ok(ApiResponse.success(
                "Agent " + request.getAgentId() + " assigned to order #" + orderId, order));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // PUT /orders/{orderId}/cancel
    // Cancel an order (Customer cancels own, Admin cancels any)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable int orderId,
            HttpServletRequest httpRequest) {

        int    customerId = extractUserId(httpRequest);
        String role       = extractRole(httpRequest);
        boolean isAdmin   = "ADMIN".equals(role);

        log.info("PUT /orders/{}/cancel â€” customerId={} isAdmin={}", orderId, customerId, isAdmin);

        OrderResponse order = orderService.cancelOrder(orderId, customerId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success(
                "Order #" + orderId + " cancelled successfully", order));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // POST /orders/{orderId}/reorder
    // Recreate a past order with one click (Customer)
    // Body: { "modeOfPayment": "COD", "deliveryAddress": "..." }
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @PostMapping("/{orderId}/reorder")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> reorder(
            @PathVariable int orderId,
            @Valid @RequestBody PlaceOrderRequest request,
            HttpServletRequest httpRequest) {

        int customerId = extractUserId(httpRequest);
        log.info("POST /orders/{}/reorder â€” customerId={}", orderId, customerId);

        OrderResponse order = orderService.reorderFromHistory(orderId, customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reorder placed successfully!", order));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // GET /orders/restaurant/{restaurantId}/analytics
    // Revenue analytics for a restaurant (Owner / Admin)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @GetMapping("/restaurant/{restaurantId}/analytics")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<ApiResponse<RevenueAnalyticsDTO>> getAnalytics(
            @PathVariable int restaurantId) {

        log.info("GET /orders/restaurant/{}/analytics", restaurantId);
        RevenueAnalyticsDTO analytics = orderService.getRevenueAnalytics(restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Revenue analytics fetched", analytics));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // GET /orders/count/{restaurantId}
    // Total order count for a restaurant (Admin)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @GetMapping("/count/{restaurantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getOrderCount(
            @PathVariable int restaurantId) {

        log.info("GET /orders/count/{}", restaurantId);
        long count = orderService.getOrderCount(restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Order count fetched", count));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // PRIVATE HELPERS â€” extract JWT claims from request attributes
    // (set by JwtAuthenticationFilter earlier in the filter chain)
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private int extractUserId(HttpServletRequest request) {
        Object id = request.getAttribute("userId");
        return id != null ? (int) id : 0;
    }

    private String extractRole(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null ? role.toString() : "";
    }

    private String extractFullName(HttpServletRequest httpRequest) {
        // Try from request attribute (set by filter if fullName claim is in token)
        Object name = httpRequest.getAttribute("fullName");
        if (name != null && !name.toString().isBlank()) return name.toString();

        // Fallback: extract from Authorization header directly
        String bearer = httpRequest.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            try {
                return jwtUtil.extractFullName(bearer.substring(7));
            } catch (Exception ignored) {}
        }
        return "Customer";
    }
}
