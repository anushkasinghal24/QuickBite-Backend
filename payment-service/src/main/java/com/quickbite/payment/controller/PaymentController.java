package com.quickbite.payment.controller;

import com.quickbite.payment.dto.request.*;
import com.quickbite.payment.dto.response.*;
import com.quickbite.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * PaymentController
 *
 * Base URL: /api/v1/payments and /api/v1/wallet
 *
 * â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * PAYMENT ENDPOINTS:
 *   POST /api/v1/payments/process          â†’ order-service calls this
 *   GET  /api/v1/payments/{id}             â†’ CUSTOMER / ADMIN
 *   GET  /api/v1/payments/order/{orderId}  â†’ CUSTOMER / order-service
 *   GET  /api/v1/payments/customer/{id}    â†’ CUSTOMER / ADMIN
 *   POST /api/v1/payments/{id}/refund      â†’ order-service / ADMIN
 *   GET  /api/v1/payments/all             â†’ ADMIN only
 *   GET  /api/v1/payments/revenue         â†’ ADMIN only
 *
 * WALLET ENDPOINTS:
 *   GET  /api/v1/wallet/{customerId}               â†’ CUSTOMER
 *   GET  /api/v1/wallet/balance/{customerId}        â†’ CUSTOMER
 *   POST /api/v1/wallet/topup                       â†’ CUSTOMER
 *   GET  /api/v1/wallet/statements/{customerId}     â†’ CUSTOMER
 * â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 *
 * Swagger: http://localhost:8084/swagger-ui.html
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment & Wallet API", description = "Payment processing, wallet management, refunds")
public class PaymentController {

    private final PaymentService paymentService;

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //                     PAYMENT ENDPOINTS
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    /**
     * POST /api/v1/payments/process
     * Called by order-service when a new order is placed.
     * Also callable by customer (for retry flow).
     *
     * Request body: { orderId, customerId, amount, mode }
     * Response: PaymentResponse with transactionId and status
     */
    @PostMapping("/api/v1/payments/process")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @Operation(summary = "Process payment for an order",
               description = "Called by order-service. Supports COD, CARD, UPI, WALLET.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request) {

        log.info("Process payment request: orderId={}, mode={}", request.getOrderId(), request.getMode());
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment processed successfully.", response));
    }

    /**
     * GET /api/v1/payments/{id}
     * Get payment by paymentId.
     */
    @GetMapping("/api/v1/payments/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @Operation(summary = "Get payment by ID", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PaymentResponse>> getByPaymentId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Payment fetched.",
                paymentService.getByPaymentId(id)));
    }

    /**
     * GET /api/v1/payments/order/{orderId}
     * Get payment by orderId â€” used by order-service to check payment status.
     */
    @GetMapping("/api/v1/payments/order/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @Operation(summary = "Get payment by orderId (used by order-service)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PaymentResponse>> getByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("Payment fetched.",
                paymentService.getByOrderId(orderId)));
    }

    /**
     * GET /api/v1/payments/customer/{customerId}?page=0&size=10
     * Get all payments for a customer (paginated, newest first).
     */
    @GetMapping("/api/v1/payments/customer/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @Operation(summary = "Get all payments for a customer",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> getByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Payments fetched.",
                paymentService.getByCustomerId(customerId, pageable)));
    }

    /**
     * POST /api/v1/payments/{id}/refund
     * Refund a payment.
     * Called by order-service when order is cancelled.
     * Also callable by ADMIN for manual refunds.
     *
     * Body: { reason, refundTo: "WALLET" | "ORIGINAL" }
     */
    @PostMapping("/api/v1/payments/{id}/refund")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @Operation(summary = "Refund a payment (on order cancellation)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable Long id,
            @RequestBody RefundRequest request) {

        request.setPaymentId(id);
        log.info("Refund request: paymentId={}, reason={}", id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Refund initiated successfully.",
                paymentService.refundPayment(id, request)));
    }

    /**
     * GET /api/v1/payments/all?page=0&size=10
     * Admin: get all payments platform-wide (paginated).
     */
    @GetMapping("/api/v1/payments/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all payments â€” ADMIN only",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> getAllPayments(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("All payments fetched.",
                paymentService.getAllPayments(pageable)));
    }

    /**
     * GET /api/v1/payments/revenue?start=2026-01-01&end=2026-12-31
     * Admin: total revenue in a date range (for analytics).
     */
    @GetMapping("/api/v1/payments/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get total revenue between dates â€” ADMIN",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Double>> getRevenue(
            @RequestParam String start,
            @RequestParam String end) {

        return ResponseEntity.ok(ApiResponse.success("Revenue calculated.",
                paymentService.getRevenueBetween(start, end)));
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    //                     WALLET ENDPOINTS
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

    /**
     * GET /api/v1/wallet/{customerId}
     * Get wallet details (auto-creates if not exists).
     */
    @GetMapping("/api/v1/wallet/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @Operation(summary = "Get wallet details for customer",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success("Wallet fetched.",
                paymentService.getOrCreateWallet(customerId)));
    }

    /**
     * GET /api/v1/wallet/balance/{customerId}
     * Get current wallet balance (lightweight endpoint).
     */
    @GetMapping("/api/v1/wallet/balance/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @Operation(summary = "Get wallet balance",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Double>> getBalance(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.success("Balance fetched.",
                paymentService.getWalletBalance(customerId)));
    }

    /**
     * POST /api/v1/wallet/topup
     * Customer adds money to wallet via CARD or UPI.
     *
     * Body: { customerId, amount, sourceMode: "CARD" | "UPI", gatewayTransactionId }
     */
    @PostMapping("/api/v1/wallet/topup")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Add money to wallet (CUSTOMER)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<WalletResponse>> topUpWallet(
            @Valid @RequestBody WalletTopUpRequest request,
            Authentication authentication) {

        // Security: ensure customer can only top-up their own wallet
        Long authenticatedUserId = (Long) authentication.getPrincipal();
        if (!authenticatedUserId.equals(request.getCustomerId())) {
            // Admin can top-up for any customer, customer can only top-up own
            log.warn("Customer {} attempted to top-up wallet of customer {}",
                    authenticatedUserId, request.getCustomerId());
            request.setCustomerId(authenticatedUserId);
        }

        log.info("Wallet top-up: customerId={}, amount={}", request.getCustomerId(), request.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Wallet topped up successfully.",
                        paymentService.addToWallet(request)));
    }

    /**
     * GET /api/v1/wallet/statements/{customerId}?page=0&size=10
     * Get wallet transaction statement history (paginated, newest first).
     */
    @GetMapping("/api/v1/wallet/statements/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    @Operation(summary = "Get wallet statement history",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<PagedResponse<WalletStatementResponse>>> getStatements(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Statements fetched.",
                paymentService.getWalletStatements(customerId, pageable)));
    }
}
